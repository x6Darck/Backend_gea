# Backups de MySQL — GEA

Tarea 3.5 del plan de migración. Responsabilidad propia: al ser una VM
on-premise, no hay snapshots automáticos de un proveedor de nube.

## Instalación en la VM de producción

1. Copiar `backup.sh` a la VM (ya vive en el repo, `deploy/backup/backup.sh`) y darle permisos de ejecución: `chmod +x backup.sh`.
2. Crear `/etc/gea-backup.env` (fuera del repo, con permisos `600`) con:
   ```bash
   DB_ROOT_PASSWORD=<contraseña real de producción>
   BACKUP_DIR=/backups
   OFFSITE_DEST=usuario@host-backup:/gea-backups/
   ```
3. Cron diario a las 2am (`crontab -e`):
   ```cron
   0 2 * * * set -a; source /etc/gea-backup.env; set +a; /ruta/a/deploy/backup/backup.sh >> /var/log/gea-backup.log 2>&1
   ```
4. Coordinar con IT (Tarea 3.2) un snapshot semanal de la VM completa vía el hipervisor — el backup de MySQL cubre los datos de la aplicación, pero no reemplaza un snapshot completo del sistema (SO, configuración, certificados).

## Restauración — procedimiento verificado

Verificado en esta sesión contra un backup real: se restauró en una base de
datos limpia y se compararon conteos de filas contra el original (coinciden
exacto). Procedimiento:

```bash
# 1. Crear una base de datos limpia para restaurar (nunca sobreescribir la real
#    directamente sin antes verificar el respaldo)
docker exec gea mysql -uroot -p"$DB_ROOT_PASSWORD" -e "CREATE DATABASE gea_restore_test;"

# 2. Restaurar el dump
gunzip -c /backups/gea_<fecha>.sql.gz | docker exec -i gea mysql -uroot -p"$DB_ROOT_PASSWORD" gea_restore_test

# 3. Verificar que los conteos coinciden con lo esperado antes de confiar en el backup
docker exec gea mysql -uroot -p"$DB_ROOT_PASSWORD" -e \
  "SELECT COUNT(*) FROM gea_restore_test.solicitudes_evento;"

# 4. Limpiar la base de prueba
docker exec gea mysql -uroot -p"$DB_ROOT_PASSWORD" -e "DROP DATABASE gea_restore_test;"
```

Para una restauración real de recuperación de desastre (reemplazar la base
de datos viva), detener el backend primero (`docker compose stop backend`),
recrear `gea` desde el dump, y arrancar el backend de nuevo — Flyway
validará el esquema restaurado contra las migraciones antes de servir
tráfico.

## Qué NO cubre este backup

- El volumen `uploads/` de archivos subidos (piezas gráficas, PDFs) — si se
  usa almacenamiento en disco local (ver hallazgo de escalabilidad sobre
  `file.upload-dir`), debe respaldarse por separado o migrarse a
  almacenamiento de objetos.
- Configuración de Nginx/certificados fuera del repo (`deploy/certs/` está
  en `.gitignore` a propósito — el certificado de producción debe respaldarse
  por el mecanismo de renovación de Let's Encrypt/CA institucional, no por
  este script).
