#!/usr/bin/env python3
"""Isolated PostgreSQL + real HTTP contract regression. Never uses real SMS or model services.

Build :server:shadowJar first, then python scripts/verify_business.py --java /path/to/java.
Only a newly-created, randomly named Docker container and JVM are stopped/removed.
"""
import argparse
import concurrent.futures
import contextlib
import http.server
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build" / "verification"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))
CHECKS = []


def port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def wait_for(check, timeout=50):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        try:
            value = check()
            if value:
                return value
        except (OSError, urllib.error.URLError, AssertionError):
            pass
        time.sleep(0.25)
    raise AssertionError("Timed out waiting for local verification service")


def verified(name, value=True):
    assert value, name
    CHECKS.append(name)
    print("PASS", name, flush=True)


class ModelStub(http.server.BaseHTTPRequestHandler):
    calls = []
    translation_failures = 1

    def log_message(self, *args):
        pass

    def do_POST(self):
        payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        ModelStub.calls.append(payload)
        translating = "翻译专家" in payload["messages"][0]["content"]
        if translating and ModelStub.translation_failures:
            ModelStub.translation_failures -= 1
            self.send_response(503)
            self.end_headers()
            return
        text = "白话译文：收到来信，愿君安好。" if translating else "友人足下：展信欣然，遥祝安好。山水之间，且待来书。"
        data = json.dumps({"choices": [{"message": {"role": "assistant", "content": text},
                                      "finish_reason": "stop"}]}, ensure_ascii=False).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default=str(Path(os.environ.get("JAVA_HOME", "")) / "bin" / ("java.exe" if os.name == "nt" else "java")))
    parser.add_argument("--keep", action="store_true", help="Keep this isolated environment running for manual UI verification")
    parser.add_argument("--api-port",type=int,default=0,help="Optional loopback port for repeatable emulator builds")
    args = parser.parse_args()
    java = args.java if Path(args.java).exists() else shutil.which("java")
    if not java or not shutil.which("docker"):
        raise SystemExit("JDK 17+ and Docker are required.")
    jar = max((ROOT / "server/build/libs").glob("*-all.jar"), key=lambda p: p.stat().st_mtime, default=None)
    if not jar:
        raise SystemExit("Build :server:shadowJar first.")
    OUT.mkdir(parents=True, exist_ok=True)
    name = "ancientpoet-verify-" + uuid.uuid4().hex[:10]
    runtime_jar = OUT / (name + ".jar")
    shutil.copy2(jar,runtime_jar)
    jar = runtime_jar
    db_port, api_port, ai_port = port(), args.api_port or port(), port()
    password = secrets.token_urlsafe(24)
    env = os.environ.copy()
    # All sensitive service configuration is replaced with isolated test values.
    env.update(POSTGRES_HOST="127.0.0.1", POSTGRES_PORT=str(db_port), POSTGRES_DB="ancientpoet",
               POSTGRES_USER="ancientpoet", POSTGRES_PASSWORD=password, APP_HOST="127.0.0.1", APP_PORT=str(api_port),
               JWT_SECRET=secrets.token_urlsafe(40), KTOR_DEVELOPMENT="true", AI_MODE="remote",
               DEV_DELIVERY_SECONDS="3", WORKER_POLL_MILLIS="1000", JOB_LEASE_SECONDS="180",
               DEEPSEEK_BASE_URL="http://127.0.0.1:" + str(ai_port), DEEPSEEK_API_KEY="local-test-only",
               JPUSH_APP_KEY="", JPUSH_MASTER_SECRET="", ENABLE_COMMUNITY="false", ENABLE_UPLOADS="false",
               SMS_ACCESS_KEY="", SMS_ACCESS_SECRET="", SMS_SDK_APP_ID="", SMS_TEMPLATE_CODE="",
               SMS_PER_PHONE_PER_DAY="2", SMS_GLOBAL_PER_DAY="4",
               HTTP_PROXY="", HTTPS_PROXY="", ALL_PROXY="", NO_PROXY="127.0.0.1,localhost")
    creation_flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    server = None
    logs = []
    ai = http.server.ThreadingHTTPServer(("127.0.0.1", ai_port), ModelStub)
    threading.Thread(target=ai.serve_forever, daemon=True).start()
    base = "http://127.0.0.1:" + str(api_port) + "/api/v1/"

    def sql(query):
        result = subprocess.run(["docker", "exec", "-i", name, "psql", "-U", "ancientpoet", "-d", env["POSTGRES_DB"],
                                 "-v", "ON_ERROR_STOP=1", "-At"], input=query, text=True, encoding="utf-8",
                                capture_output=True, check=True, creationflags=creation_flags)
        return result.stdout.strip()

    def api(path, method="GET", data=None, token=None, expected=200):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        req = urllib.request.Request(base + path, data=None if data is None else json.dumps(data).encode(),
                                     headers=headers, method=method)
        try:
            response = OPENER.open(req, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read().decode()
            assert response.status == expected, (method, path, response.status, body[:180])
            return json.loads(body) if body else None

    def start():
        nonlocal server
        log = (OUT / ("server-" + str(len(logs)) + ".log")).open("w", encoding="utf-8")
        logs.append(log)
        server = subprocess.Popen([java, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-jar", str(jar)], env=env, cwd=ROOT, stdout=log, stderr=log,
                                  creationflags=creation_flags)
        wait_for(lambda: api("health/ready"))

    def stop():
        nonlocal server
        if server:
            server.terminate()
            try:
                server.wait(timeout=15)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait(timeout=10)
            server = None

    try:
        subprocess.run(["docker", "run", "--detach", "--rm", "--name", name,
                        "-e", "POSTGRES_PASSWORD", "-e", "POSTGRES_USER=ancientpoet", "-e", "POSTGRES_DB=ancientpoet",
                        "-p", "127.0.0.1:" + str(db_port) + ":5432", "postgis/postgis:16-3.4"],
                       env=env, check=True, capture_output=True, creationflags=creation_flags)
        wait_for(lambda: subprocess.run(["docker", "exec", name, "pg_isready", "-h", "127.0.0.1", "-U", "ancientpoet"],
                                       capture_output=True, creationflags=creation_flags).returncode == 0)
        start()
        verified("Fresh Flyway migration and PostgreSQL readiness")
        stop()
        sql("CREATE DATABASE ancientpoet_upgrade;")
        env["POSTGRES_DB"] = "ancientpoet_upgrade"
        prepared = subprocess.run([java, "-cp", str(jar), str(ROOT / "scripts/support/PrepareUpgrade.java")],
            env=env, capture_output=True, text=True, encoding="utf-8", creationflags=creation_flags)
        assert prepared.returncode == 0, "Could not prepare old-schema fixture: " + prepared.stderr[-1000:]
        start()
        assert sql("SELECT content_text FROM messages WHERE is_delivered=true") == "legacy letter preserved"
        assert sql("SELECT count(*) FROM conversations c JOIN poets p ON p.id=c.poet_id WHERE c.storyline_current_year BETWEEN p.birth_year AND p.death_year") == "1"
        assert sql("SELECT count(*) FROM conversation_summaries") == "1"
        verified("V3 to current upgrade preserves letters and summaries, repairs delivery and year")
        stop()
        env["POSTGRES_DB"] = "ancientpoet"
        start()
        verified("Optional upload/community endpoints disabled", api("info")["uploadsEnabled"] is False)
        api("community/posts", expected=404)
        api("auth/sms/send", "POST", {"phone": "123"}, expected=400)
        a = api("auth/sms/verify", "POST", {"phone": "13800000001", "code": "123456"})
        b = api("auth/sms/verify", "POST", {"phone": "13800000002", "code": "123456"})
        token = a["accessToken"]
        api("user/profile", token=a["refreshToken"], expected=401)
        api("auth/refresh", "POST", {"refreshToken": token}, expected=401)
        verified("Access and refresh tokens isolated; phone validation")
        poets = api("poets")["poets"]
        verified("15 poets preserved", len(poets) == 15)
        for poet in poets:
            detail = api("poets/" + str(poet["id"]))
            assert detail["personalityProfile"]["traits"], poet["name"]
            location = api("poets/" + str(poet["id"]) + "/location")
            assert poet["birthYear"] <= location["year"] <= poet["deathYear"]
        verified("All poet profiles parse and default years stay within lifespan")
        for poet in poets[:5]:
            assert api("poets/" + str(poet["id"]) + "/poems"), poet["name"]
        poems = api("poems")
        assert all(api("poems/" + str(p["id"]))["content"] == p["content"] for p in poems)
        assert api("poems/search?q=" + urllib.parse.quote("明月"))
        verified("Core poets have full-text poems; direct details and search serialize")
        tang, song = api("map/tang/cities"), api("map/song/cities")
        assert tang != song
        api("map/nonexistent/cities", expected=404)
        city = tang[0]
        conversation = api("conversations", "POST", {"poetId": poets[0]["id"]}, token, 201)
        conv_id = conversation["id"]
        path = "conversations/" + str(conv_id)
        api(path + "/messages", "POST", {"contentText": "尚未选择位置"}, token, 409)
        for suffix in ("", "/messages", "/pending", "/storyline/state", "/delivery-preview"):
            api(path + suffix, token=b["accessToken"], expected=404)
        api(path + "/storyline/jump", "POST", {"year": 743}, b["accessToken"], 404)
        api(path, "DELETE", token=b["accessToken"], expected=404)
        verified("Conversation ownership enforced across read/write/storyline")
        api("user/location/tang", "PUT", {"locationName": city["name"], "lat": 0, "lng": 0}, token, 400)
        api("user/location/tang", "PUT", {"locationName": city["name"], "lat": city["lat"], "lng": city["lng"]}, token)
        verified("Dynasty cities differ and coordinates are server-validated")
        quote = api(path + "/delivery-preview", token=token)
        submission = {"contentText": "这是第一封测试书信。", "clientMessageId": uuid.uuid4().hex}
        receipt = api(path + "/messages", "POST", submission, token)
        assert receipt["messageId"] > 0
        assert receipt["estimatedDelivery"]["delaySeconds"] == quote["delaySeconds"]
        retry = api(path + "/messages", "POST", submission, token)
        assert retry["messageId"] == receipt["messageId"]
        api(path + "/messages", "POST", dict(submission, contentText="changed"), token, 409)
        messages = api(path + "/messages", token=token)
        assert any(m["id"] == receipt["messageId"] and m["isDelivered"] for m in messages)
        pending = api(path + "/pending", token=token)
        assert pending and all("contentText" not in p and "translation" not in p for p in pending)
        verified("ACK contains real ID; user letter visible; retries idempotent; pending hides body")
        wait_for(lambda: len(api(path + "/messages", token=token)) == 2)
        original = api(path + "/messages", token=token)[1]
        assert original["contentText"]
        verified("Original arrives even when translation service fails")
        generated = [c for c in ModelStub.calls if "虚构回信" in c["messages"][0]["content"]]
        assert len(generated) == 1
        assert sum(m["content"] == submission["contentText"] for m in generated[0]["messages"]) == 1
        sql("UPDATE message_jobs SET next_attempt_at=now() WHERE kind='translate' AND state='retrying';")
        wait_for(lambda: api(path + "/messages", token=token)[1]["translation"])
        verified("Independent translation retry succeeds; new user input occurs once in AI context")
        inbox = api("conversations", token=token)
        assert inbox[0]["unreadCount"] == 1 and inbox[0]["poet"]["name"]
        assert inbox[0]["latestUnreadMessageId"] == original["id"]
        api(path + "/read", "POST", {"throughId": original["id"]}, token, 204)
        assert api("conversations", token=token)[0]["unreadCount"] == 0
        verified("Inbox poet name, unread count, and read receipt agree")
        # Restart with a persisted, expired running lease; completion must be unique.
        stop()
        mid = receipt["messageId"]
        sql("DELETE FROM messages WHERE reply_to_message_id=" + str(mid) + ";"
            "UPDATE message_jobs SET state='running',lease_token='expired-test',lease_until=now()-interval '1 second' WHERE message_id=" + str(mid) + " AND kind='generate';")
        start()
        wait_for(lambda: len(api(path + "/messages", token=token)) == 2)
        assert sql("SELECT count(*) FROM messages WHERE reply_to_message_id=" + str(mid)) == "1"
        verified("Restart recovers expired PostgreSQL lease with one persisted reply")
        duplicate = {"contentText": "并发重试书信", "clientMessageId": uuid.uuid4().hex}
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            receipts = list(executor.map(lambda _: api(path + "/messages", "POST", duplicate, token), range(4)))
        assert len({item["messageId"] for item in receipts}) == 1
        verified("Concurrent duplicate submissions create one letter/job")
        # Cursor history is checked beyond one page.
        sql("INSERT INTO messages(conversation_id,sender_type,content_text,is_delivered,delivered_at) "
            "SELECT " + str(conv_id) + ",'user','分页验证 '||n,true,now() FROM generate_series(1,70) n;")
        latest = api(path + "/messages", token=token)
        assert len(latest) == 50 and latest[-1]["contentText"] == "分页验证 70"
        older = api(path + "/messages?beforeId=" + str(latest[0]["id"]), token=token)
        assert older and max(m["id"] for m in older) < latest[0]["id"]
        verified("Latest history and older cursor preserve chronological order beyond 50 messages")
        other = next(c for c in tang if c["name"] != city["name"])
        preview = api("user/location/tang/move-preview", "POST", {"toName": other["name"], "toLat": other["lat"], "toLng": other["lng"]}, token)
        assert 3600 <= preview["travelSeconds"] <= 7*86400
        moved = api("user/location/tang/move", "POST", {"toName": other["name"], "toLat": other["lat"], "toLng": other["lng"]}, token)
        assert moved["status"] == "moving" and moved["movingToName"] == other["name"]
        status = api("user/location/tang/status", token=token)
        assert status["remainingSeconds"] > 0 and status["movingToName"] == other["name"]
        api("user/location/tang/move", "POST", {"toName": city["name"], "toLat": city["lat"], "toLng": city["lng"]}, token, 409)
        sql("UPDATE user_locations SET moving_arrival_time=now()-interval '1 second' WHERE user_id=" + str(a["userId"]))
        assert api("user/location/tang/status", token=token)["status"] == "settled"
        verified("Travel state persists, concurrent departure rejected, arrival settles")
        api(path + "/archive", "POST", {"archived": True}, token, 204)
        assert api(path + "/messages","POST",submission,token)["messageId"] == receipt["messageId"]
        assert not api("conversations", token=token)
        assert api("conversations?archived=true", token=token)[0]["id"] == conv_id
        api(path + "/archive", "POST", {"archived": False}, token, 204)
        exported = api("user/export", token=token)
        assert all(c["user_id"] == a["userId"] for c in exported["conversations"])
        api(path, "DELETE", token=token, expected=204)
        assert sql("SELECT count(*) FROM messages WHERE conversation_id=" + str(conv_id)) == "0"
        verified("Archive/restore, idempotent receipt after archive, private export, and cascade deletion")
        rotated = api("auth/refresh", "POST", {"refreshToken": a["refreshToken"]})
        assert rotated["refreshToken"] != a["refreshToken"]
        api("user/profile", token=rotated["accessToken"])
        api("auth/refresh", "POST", {"refreshToken": a["refreshToken"]}, expected=401)
        api("user/profile", token=rotated["accessToken"], expected=401)
        verified("Refresh rotation and reuse revoke the compromised session")
        api("auth/logout", "POST", token=b["accessToken"], expected=204)
        api("user/profile", token=b["accessToken"], expected=401)
        c = api("auth/sms/verify", "POST", {"phone": "13800000003", "code": "123456"})
        api("user/account", "DELETE", token=c["accessToken"], expected=204)
        api("user/profile", token=c["accessToken"], expected=401)
        verified("Logout and account deletion invalidate active sessions")
        stop()
        env.update(KTOR_DEVELOPMENT="false", DEEPSEEK_BASE_URL="https://unused.invalid", DEV_DELIVERY_SECONDS="")
        env.pop("DEV_DELIVERY_SECONDS")
        start()
        assert api("info")["development"] is False
        api("auth/sms/verify","POST",{"phone":"13800000888","code":"123456"},expected=401)
        for _ in range(2):
            api("auth/sms/send","POST",{"phone":"13800000888"},expected=503)
        api("auth/sms/send","POST",{"phone":"13800000888"},expected=429)
        assert sql("SELECT count(*) FROM sms_challenges WHERE phone='13800000888'") == "0"
        verified("Production has no demo-code bypass; missing SMS provider fails and daily budget persists")
        stop()
        env.update(KTOR_DEVELOPMENT="true", DEEPSEEK_BASE_URL="http://127.0.0.1:" + str(ai_port), DEV_DELIVERY_SECONDS="3")
        start()
        print("VERIFIED", len(CHECKS), "business gates", flush=True)
        (OUT / "business-results.json").write_text(json.dumps({"passed": CHECKS}, ensure_ascii=False, indent=2), encoding="utf-8")
        if args.keep:
            # No secrets or tokens are written. This test service only accepts explicit demo phones.
            (OUT / "manual-environment.json").write_text(json.dumps({"baseUrl": base, "container": name,
                "serverPid": server.pid, "aiPort": ai_port}, indent=2), encoding="utf-8")
            print("Local UI verification API:", base, flush=True)
            print("Press Ctrl+C to stop and clean this owned environment.", flush=True)
            stop_file=OUT / "stop-manual"
            if stop_file.exists():
                stop_file.unlink()
            while not stop_file.exists():
                time.sleep(1)
            stop_file.unlink()
    finally:
        stop()
        for log in logs:
            log.close()
        ai.shutdown()
        subprocess.run(["docker", "rm", "-f", name], capture_output=True, creationflags=creation_flags)
        if runtime_jar.exists():
            runtime_jar.unlink()


if __name__ == "__main__":
    main()
