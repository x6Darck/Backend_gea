#!/usr/bin/env bash
# Backup diario de MySQL — responsabilidad propia en la VM on-premise (no hay
# snapshots automáticos de nube). Pensado para correr vía cron (Tarea 3.5,
# Paso 2) dentro de la propia VM, junto al docker-compose.
#
# Uso: DB_ROOT_PASSWORD=xxx ./backup.sh
# Variables de entorno (todas opcionales salvo DB_ROOT_PASSWORD):
#   DB_ROOT_PASSWORD   contraseña root de MySQL (requerida)
#   DB_CONTAINER       nombre del contenedor MySQL (default: gea)
#   DB_NAME            base de datos a respaldar (default: gea)
#   BACKUP_DIR         directorio local de respaldos (default: /backups)
#   RETENTION_DAYS     días de retención local (default: 7)
#   OFFSITE_DEST       destino rsync fuera de la VM, ej. usuario@host:/ruta
#                      (si no se define, se omite la copia offsite con un
#                      aviso — no falla el backup local)
set -euo pipefail

: "${DB_ROOT_PASSWORD:?Debes definir DB_ROOT_PASSWORD}"
DB_CONTAINER="${DB_CONTAINER:-gea}"
DB_NAME="${DB_NAME:-gea}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
OFFSITE_DEST="${OFFSITE_DEST:-}"

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%F_%H%M%S)
DEST_FILE="$BACKUP_DIR/gea_${STAMP}.sql.gz"

echo "[backup] Volcando ${DB_NAME} desde el contenedor ${DB_CONTAINER}..."
docker exec "$DB_CONTAINER" mysqldump \
  -u root -p"$DB_ROOT_PASSWORD" \
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
  echo "[backup] AVISO: OFFSITE_DEST no está definido — el respaldo solo existe en esta VM." >&2
  echo "[backup] Un respaldo que vive solo en el mismo servidor que respalda no protege contra fallo de disco/VM." >&2
fi

echo "[backup] Listo."
