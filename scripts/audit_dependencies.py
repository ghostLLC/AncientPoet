#!/usr/bin/env python3
"""Query OSV for resolved Maven versions from Gradle dependency reports.
Only public package names/versions are sent. No source, credentials or lockfile contents are uploaded.
Usage: python scripts/audit_dependencies.py report1.txt report2.txt [--fail-on-match]
"""
import argparse
import json
import re
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reports",nargs="+",type=Path)
    parser.add_argument("--fail-on-match",action="store_true")
    args=parser.parse_args()
    text="\n".join(p.read_text(encoding="utf-8-sig") for p in args.reports)
    coordinates=set()
    for match in re.finditer(r"(?:\+---|\\---) ([\w.\-]+):([\w.\-]+):([\w.\-]+)(?: -> ([\w.\-]+))?",text):
        group,artifact,declared,selected=match.groups()
        coordinates.add((group+":"+artifact,selected or declared))
    if not coordinates:
        raise SystemExit("No resolved Maven coordinates were found.")
    packages=[{"package":{"ecosystem":"Maven","name":name},"version":version} for name,version in sorted(coordinates)]
    request=urllib.request.Request("https://api.osv.dev/v1/querybatch",
        data=json.dumps({"queries":packages}).encode(),headers={"Content-Type":"application/json"},method="POST")
    with urllib.request.urlopen(request,timeout=45) as response:
        matches=json.load(response)["results"]
    hits=[dict(package,advisories=[v["id"] for v in result.get("vulns",[])])
          for package,result in zip(packages,matches) if result.get("vulns")]
    out=Path(__file__).resolve().parents[1]/"build/verification/dependency-audit.json"
    out.parent.mkdir(parents=True,exist_ok=True)
    result={"checkedAt":datetime.now(timezone.utc).isoformat(),"packageVersions":len(packages),"matches":hits,
        "scope":"Resolved Maven runtime versions in supplied reports. Not an exploitability, container-image or complete security audit."}
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False,indent=2))
    if hits and args.fail_on_match:
        raise SystemExit(1)

if __name__=="__main__":
    main()
