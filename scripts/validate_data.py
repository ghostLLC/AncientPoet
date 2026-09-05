#!/usr/bin/env python3
"""Validate checked-in editorial JSON. Runtime seed data is tested separately against PostgreSQL."""
import json
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))
def location(item):
    assert -90<=item["lat"]<=90 and -180<=item["lng"]<=180
def main():
    poets=[read(p) for p in sorted((ROOT/"data/poets").glob("*.json"))]
    assert len(poets)==15 and len({p["id"] for p in poets})==15
    for poet in poets:
        assert poet["birthYear"]<=poet["deathYear"] and poet["personalityProfile"]["traits"]
        for movement in poet["movements"]:
            location(movement)
            assert movement["yearStart"]<=movement["yearEnd"]
    cities=[read(p) for p in sorted((ROOT/"data/cities").glob("*.json"))]
    assert {c["dynasty"] for c in cities}=={"tang","song","han","jin","ming"}
    for dynasty in cities:
        names=[c["name"] for c in dynasty["cities"]]
        assert len(set(names))==len(names)
        for city in dynasty["cities"]: location(city)
    curated=read(ROOT/"data/curated_poems.json")
    assert Counter(p["poet"] for p in curated)=={n:2 for n in ("李白","杜甫","苏轼","李清照","王维")}
    assert len({(p["poet"],p["title"]) for p in curated})==10
    seed=(ROOT/"server/src/main/resources/db/migration/V5__curated_poems.sql").read_text(encoding="utf-8")
    for poem in curated:
        assert poem["sourceUrl"].startswith("https://zh.wikisource.org/wiki/") and poem["reviewedAt"]
        for key in ("content","title","translation","appreciation","sourceUrl"):
            assert poem[key] and poem[key].replace("'","''") in seed, (poem["title"],key)
        assert "……" not in poem["content"] and "..." not in poem["content"]
    print("PASS 15 poet files, 5 dynasty city files, 10 reviewed poems and migration consistency")

if __name__=="__main__": main()
