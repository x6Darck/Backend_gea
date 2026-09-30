#!/usr/bin/env bash
# Backup diario de MySQL para despliegue NATIVO (sin Docker) — responsabilidad
# propia en la VM on-premise (no hay snapshots automáticos de nube). Pensado
# para correr vía cron. Misma lógica que deploy/backup/backup.sh (versión
# Docker), sin el paso de docker exec.
#
# Uso: DB_PASSWORD=xxx ./backup-native.sh
# Variables de entorno (todas opcionales salvo DB_PASSWORD):
#   DB_USER            usuario de MySQL con permiso de lectura (default: gea_app)
#   DB_PASSWORD        contraseña de ese usuario (requerida)
#   DB_NAME            base de datos a respaldar (default: gea)
#   BACKUP_DIR         directorio local de respaldos (default: /backups)
#   RETENTION_DAYS     días de retención local (default: 7)
#   OFFSITE_DEST       destino rsync fuera del servidor, ej. usuario@host:/ruta
#                      (si no se define, se omite la copia offsite con un
#                      aviso — no falla el backup local)
set -euo pipefail

: "${DB_PASSWORD:?Debes definir DB_PASSWORD}"
DB_USER="${DB_USER:-gea_app}"
DB_NAME="${DB_NAME:-gea}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
OFFSITE_DEST="${OFFSITE_DEST:-}"

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%F_%H%M%S)
DEST_FILE="$BACKUP_DIR/gea_${STAMP}.sql.gz"

echo "[backup] Volcando ${DB_NAME} (mysqldump local)..."
mysqldump \
  -u "$DB_USER" -p"$DB_PASSWORD" \
  --single-transaction --routines --triggers --events \
  "$DB_NAME" | gzip > "$DEST_FILE"

SIZE=$(du -h "$DEST_FILE" | cut -f1)
echo "[backup] Volcado completo: $DEST_FILE ($SIZE)"

echo "[backup] Purgando respaldos locales de más de ${RETENTION_DAYS} días..."
find "$BACKUP_DIR" -name 'gea_*.sql.gz' -mtime "+${RETENTION_DAYS}" -print -delete

if [ -n "$OFFSITE_DEST" ]; then
  echo "[backup] Copiando a destino offsite: ${OFFSITE_DEST}"
  rsync -a "$DEST_FILE" "$OFFSITE_DEST"
else
  echo "[backup] AVISO: OFFSITE_DEST no está definido — el respaldo solo existe en este servidor." >&2
  echo "[backup] Un respaldo que vive solo en el mismo servidor que respalda no protege contra fallo de disco." >&2
fi

echo "[backup] Listo."
