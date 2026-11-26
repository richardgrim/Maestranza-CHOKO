"""Clasifica las coincidencias del JSON de Trivy sin descartar paquetes repetidos."""
import csv
import json
import sys
from collections import Counter
from pathlib import Path

informe, salida = map(Path, sys.argv[1:])
salida.mkdir(parents=True, exist_ok=True)
datos = json.loads(informe.read_text())
filas = []
for objetivo in datos["Results"]:
    for v in objetivo.get("Vulnerabilities", []):
        filas.append([objetivo["Target"], objetivo["Type"], v["PkgName"], v["InstalledVersion"],
                      v["VulnerabilityID"], v["Severity"], v.get("FixedVersion", ""), v.get("Status", "")])
with (salida / "hallazgos.csv").open("w", newline="") as archivo:
    tabla = csv.writer(archivo)
    tabla.writerow(["objetivo", "tipo", "paquete", "instalada", "identificador", "gravedad", "corregida", "estado"])
    tabla.writerows(sorted(filas))

paquetes = Counter(f[2] for f in filas)
corregibles = sorted({(f[2], f[3], f[6]) for f in filas if f[6]})
resumen = {
    "imagen": datos["Metadata"].get("ImageID"),
    "sistema": datos["Metadata"].get("OS"),
    "total": len(filas),
    "identificadores_distintos": len({f[4] for f in filas}),
    "por_gravedad": dict(Counter(f[5] for f in filas)),
    "por_tipo": dict(Counter(f[1] for f in filas)),
    "por_estado": dict(Counter(f[7] for f in filas)),
    "con_version_corregida": sum(bool(f[6]) for f in filas),
    "principales_paquetes": paquetes.most_common(12),
    "actualizaciones_identificadas": corregibles,
    "detalle_corregible": [f for f in filas if f[6]],
}
texto = json.dumps(resumen, ensure_ascii=False, indent=2)
(salida / "resumen.json").write_text(texto + "\n")
print("TRIVY_RESUMEN " + json.dumps(resumen, ensure_ascii=False))
