"""Update compact NASA inputs using requests and the team JSON contract."""
from __future__ import annotations
import datetime as dt
import json
from pathlib import Path
import requests

ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/"data"
RAW=DATA/"flares_raw.json"
# api.nasa.gov/DONKI/FLR devuelve HTTP 404; el endpoint público vigente es el de CCMC (no requiere clave).
DONKI="https://ccmc.gsfc.nasa.gov/DONKI-API/get/FLR"
OSDR="https://visualization.osdr.nasa.gov/biodata/api/v2/query/assays/"
RADLAB="https://visualization.osdr.nasa.gov/radlab/api/"
EDA="https://visualization.osdr.nasa.gov/eda/"
TIMEOUT=30
STUDIES=["https://osdr.nasa.gov/bio/repo/data/studies/OSD-980","https://osdr.nasa.gov/bio/repo/data/studies/OSD-781"]

def records(payload):
    if isinstance(payload,dict) and isinstance(payload.get("columns"),list):
        return [dict(zip(payload["columns"],row)) for row in payload.get("data",[]) if isinstance(row,(list,tuple))]
    if isinstance(payload,list):
        return [row for row in payload if isinstance(row,dict)]
    return []

def get_flares(session,start,end):
    try:
        r=session.get(DONKI,params={"startDate":start.isoformat(),"endDate":end.isoformat()},timeout=TIMEOUT)
        r.raise_for_status()
        raw=r.json()
        if not isinstance(raw,list):
            raise ValueError("DONKI response was not a JSON list")
        RAW.write_text(json.dumps(raw,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
        return [{"date":x["beginTime"][:10],"class":x["classType"]} for x in raw if x.get("beginTime") and x.get("classType")],None
    except (requests.RequestException,ValueError,KeyError) as exc:
        return [],str(exc)

def get_osdr_sources(session):
    try:
        r=session.get(OSDR,params={"study.characteristics.organism":"mus musculus","format":"json"},timeout=TIMEOUT)
        r.raise_for_status()
        rows=records(r.json())
        selected=[]
        for row in rows:
            text=" ".join(str(v) for v in row.values()).lower()
            accession=str(row.get("id.accession",""))
            if accession and any(word in text for word in ("bone","muscle","skeletal","microgravity","spaceflight")):
                url="https://osdr.nasa.gov/bio/repo/data/studies/"+accession
                if url not in selected:
                    selected.append(url)
            if len(selected)>=6:
                break
        return selected or STUDIES,None
    except (requests.RequestException,ValueError) as exc:
        return STUDIES,str(exc)

def get_radiation(session,start,end):
    try:
        p={"spacecraft":"ISS","timestamp>":start.isoformat(),"timestamp<":end.isoformat(),"timestamp":"","dose_equivalent_rate":"","format":"json"}
        r=session.get(RADLAB,params=p,timeout=TIMEOUT)
        r.raise_for_status()
        values=[]
        for row in records(r.json()):
            try:
                if row.get("dose_equivalent_rate") is not None:
                    values.append(float(row["dose_equivalent_rate"]))
            except (TypeError,ValueError):
                pass
        if values:
            return round(sum(values)/len(values),6),"measured",None
        return 28.0,"provisional_example_no_recent_readings",None
    except requests.RequestException as exc:
        return 28.0,"provisional_example_api_error: "+str(exc),str(exc)

def main():
    today=dt.datetime.now(dt.timezone.utc).date()
    start=today-dt.timedelta(days=30)
    s=requests.Session()
    s.headers["User-Agent"]="AstroVital NASA data ingestion"
    flares,flare_error=get_flares(s,start,today)
    osdr_sources,osdr_error=get_osdr_sources(s)
    radiation,radiation_status,radiation_error=get_radiation(s,start,today+dt.timedelta(days=1))
    output={
        "generated":today.isoformat(),
        "mission_day":112,
        "radiation_uSv_h":radiation,
        "cabin_co2_mmHg":3.8,
        "solar_flares_30d":flares,
        "sources":[DONKI,"https://visualization.osdr.nasa.gov/biodata/api/",OSDR,*osdr_sources,RADLAB,EDA],
        "_quality":{
            "mission_day":"Provisional example from the team contract.",
            "radiation_uSv_h":radiation_status,
            "cabin_co2_mmHg":"Provisional example from the team contract; EDA CSV not yet imported.",
            "solar_flares_30d":"unavailable" if flare_error else "observed",
            "solar_flares_error":flare_error,
            "osdr_metadata_error":osdr_error,
            "radiation_error":radiation_error,
            "note":"An empty flare list with status unavailable does not mean zero events."
        }
    }
    (DATA/"risk_inputs.json").write_text(json.dumps(output,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print("Wrote data/risk_inputs.json")

if __name__=="__main__":
    main()