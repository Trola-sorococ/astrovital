"""Download compact NASA inputs and write data/risk_inputs.json."""
from __future__ import annotations
import datetime as dt
import json
import os
from pathlib import Path
from typing import Any
import requests

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "data"
DONKI = "https://api.nasa.gov/DONKI/FLR"
OSDR = "https://visualization.osdr.nasa.gov/biodata/api/v2/query/assays/"
RADLAB = "https://visualization.osdr.nasa.gov/radlab/api/"
KEY = os.getenv("NASA_API_KEY", "DEMO_KEY")
TIMEOUT = 30
BASELINE_STUDIES = [
    {"accession":"OSD-980","title":"Cold stress during room temperature housing alters skeletal response to simulated microgravity (hindlimb unloading) in growing female C57BL6 mice","organism":"Mus musculus","model":"hindlimb unloading (simulated microgravity)","focus":"bone","source_url":"https://osdr.nasa.gov/bio/repo/data/studies/OSD-980"},
    {"accession":"OSD-781","title":"Skeletal muscle-on-a-chip in microgravity as a platform for regeneration modeling and drug screening - myoblast data","organism":"Homo sapiens","model":"spaceflight / microgravity","focus":"skeletal muscle","source_url":"https://osdr.nasa.gov/bio/repo/data/studies/OSD-781"},
]

def records(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, list):
        return [row for row in payload if isinstance(row, dict)]
    if isinstance(payload, dict) and isinstance(payload.get("columns"), list) and isinstance(payload.get("data"), list):
        return [dict(zip(payload["columns"], row)) for row in payload["data"] if isinstance(row, (list, tuple))]
    if isinstance(payload, dict) and isinstance(payload.get("data"), list):
        return [row for row in payload["data"] if isinstance(row, dict)]
    return []

def get_flares(session: requests.Session, start: dt.date, end: dt.date):
    try:
        response = session.get(DONKI, params={"startDate":start.isoformat(),"endDate":end.isoformat(),"api_key":KEY}, timeout=TIMEOUT)
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):
            raise ValueError("DONKI FLR response was not a JSON list")
        (DATA / "flares_raw.json").write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        return payload, None
    except (requests.RequestException, ValueError) as exc:
        return None, str(exc)

def get_studies(session: requests.Session):
    try:
        response = session.get(OSDR, params={"study.characteristics.organism":"mus musculus","format":"json"}, timeout=TIMEOUT)
        response.raise_for_status()
        items = records(response.json())
        terms = ("bone","muscle","skeletal","microgravity","spaceflight")
        selected, seen = [], set()
        for row in items:
            text = " ".join(str(value) for value in row.values()).lower()
            accession = str(row.get("id.accession", ""))
            if accession and accession not in seen and any(term in text for term in terms):
                selected.append(row)
                seen.add(accession)
            if len(selected) == 6:
                break
        return selected, None
    except (requests.RequestException, ValueError) as exc:
        return None, str(exc)

def get_radlab(session: requests.Session, start: dt.date, end: dt.date):
    params = {"spacecraft":"ISS","instrument":"DosTel","timestamp>":start.isoformat(),"timestamp<":end.isoformat(),"timestamp":"","absorbed_dose_rate":"","format":"json"}
    response = session.get(RADLAB, params=params, timeout=TIMEOUT)
    response.raise_for_status()
    return records(response.json())

def radiation_summary(rows, start, end, historical=False):
    values = []
    for row in rows:
        try:
            if row.get("absorbed_dose_rate") is not None:
                values.append(float(row["absorbed_dose_rate"]))
        except (TypeError, ValueError):
            pass
    if not values:
        return {"status":"unavailable","sample_count":0,"period_start":start.isoformat(),"period_end_exclusive":end.isoformat(),"source_url":RADLAB}
    return {
        "status":"historical_reference" if historical else "observed",
        "sample_count":len(values),
        "period_start":start.isoformat(),
        "period_end_exclusive":end.isoformat(),
        "period_kind":"historical_fallback" if historical else "recent_window",
        "absorbed_dose_rate_mean_ugy_h":round(sum(values)/len(values),6),
        "absorbed_dose_rate_max_ugy_h":round(max(values),6),
        "source_url":RADLAB + "?spacecraft=ISS&instrument=DosTel&timestamp%3E=" + start.isoformat() + "&timestamp%3C" + end.isoformat() + "&absorbed_dose_rate&timestamp&format=json",
    }

def main():
    DATA.mkdir(parents=True, exist_ok=True)
    today = dt.datetime.now(dt.timezone.utc).date()
    start, end = today - dt.timedelta(days=30), today
    session = requests.Session()
    session.headers["User-Agent"] = "AstroVital/1.0 NASA data ingestion"
    warnings = []

    flares, flare_error = get_flares(session, start, end)
    if flare_error:
        warnings.append("DONKI FLR unavailable: " + flare_error)

    studies, osdr_error = get_studies(session)
    if osdr_error:
        warnings.append("OSDR metadata query unavailable: " + osdr_error)
    studies = studies or BASELINE_STUDIES

    radiation = None
    try:
        rows = get_radlab(session, start, end + dt.timedelta(days=1))
        if rows:
            radiation = radiation_summary(rows, start, end + dt.timedelta(days=1))
    except requests.RequestException as exc:
        warnings.append("RadLab recent interval unavailable: " + str(exc))
    if radiation is None:
        d0 = dt.date(2023, 1, 1)
        try:
            rows = get_radlab(session, d0, d0 + dt.timedelta(days=1))
            radiation = radiation_summary(rows, d0, d0 + dt.timedelta(days=1), historical=True)
        except requests.RequestException as exc:
            warnings.append("RadLab historical fallback unavailable: " + str(exc))
    radiation = radiation or {"status":"unavailable","source_url":RADLAB}

    output = {
        "schema_version":"1.0",
        "generated_at_utc":dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat(),
        "data_status":"live" if flares is not None else "provisional_partial",
        "inputs":{
            "solar_flares_last_30_days":{
                "value":len(flares) if flares is not None else None,
                "unit":"events","period_start":start.isoformat(),"period_end_inclusive":end.isoformat(),
                "status":"observed" if flares is not None else "unavailable",
                "source_url":DONKI,"source_error":flare_error,
                "note":"null means the API did not return data; it does not mean zero flares."
            },
            "iss_dosetel_absorbed_dose_rate":{**radiation,"unit":"µGy/hour","instrument":"DosTel","spacecraft":"ISS"},
            "musculoskeletal_microgravity_studies":{
                "value":studies,"status":"observed" if studies and studies != BASELINE_STUDIES else "curated_fallback",
                "source_url":OSDR,"source_error":osdr_error,
                "note":"Qualitative evidence only, not a clinical risk score."
            }
        },
        "warnings":warnings
    }
    (DATA / "risk_inputs.json").write_text(json.dumps(output, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("Wrote data/risk_inputs.json")

if __name__ == "__main__":
    main()