# Comandos de operación en Linux — GEA

Referencia rápida para administrar el servidor de producción (XAMPP: Apache +
MariaDB, backend Spring Boot como servicio de `systemd`). Todos los comandos
son para copiar y pegar tal cual, ajustando solo rutas si el servidor real
las tiene distintas.

---

## 1. Backend (`gea-backend`, servicio de systemd)

```bash
# Ver estado (arriba/abajo, hace cuánto arrancó, últimas líneas de log)
sudo systemctl status gea-backend

# Detener
sudo systemctl stop gea-backend

# Iniciar
sudo systemctl start gea-backend

# Reiniciar (detener + iniciar en un solo paso — usar esto después de
# cualquier cambio en /etc/gea/backend.env)
sudo systemctl restart gea-backend

# Ver los logs en vivo (Ctrl+C para salir, no detiene el backend)
sudo journalctl -u gea-backend -f

# Ver las últimas 100 líneas del log, sin quedarse esperando
sudo journalctl -u gea-backend -n 100 --no-pager

# Ver los logs de un rango de tiempo (útil para revisar qué pasó anoche)
sudo journalctl -u gea-backend --since "2026-07-14 08:00" --until "2026-07-14 09:00"

# Confirmar que responde (debe devolver {"status":"UP"} o similar)
curl http://127.0.0.1:8084/actuator/health

# Que arranque solo si el servidor se reinicia (normalmente ya viene así)
sudo systemctl enable gea-backend
```

**Nunca usar `mvn package`/`mvn clean` para "apagar" el backend** — eso solo
recompila el código, no toca el proceso que está corriendo. Para
detener/iniciar el backend real, siempre `systemctl stop/start/restart`.

## 2. Editar variables de entorno (`.env` real del backend)

```bash
sudo nano /etc/gea/backend.env
```

Después de **cualquier** cambio en este archivo, hay que reiniciar para que
tome efecto:

```bash
sudo systemctl restart gea-backend
sudo journalctl -u gea-backend -f   # confirmar que arrancó sin errores
```

## 6. Actualizar el backend a una nueva versión de código

```bash
cd /opt/proyectos/GEA_BACKEND-main   # ruta del checkout, ajustar si es otra
git pull
./mvnw clean package -DskipTests

sudo systemctl stop gea-backend
sudo cp target/callapp_backend-0.0.1-SNAPSHOT.jar /opt/gea/backend/app.jar
sudo chown gea:gea /opt/gea/backend/app.jar
sudo systemctl start gea-backend

sudo journalctl -u gea-backend -f   # confirmar arranque limpio (Flyway al día, sin errores)
```

## 7. Actualizar el frontend

```bash
cd /opt/proyectos/GEA_FRONT-main   # ajustar ruta real
git pull
nano .env.production   # confirmar que VITE_API_URL sigue apuntando al dominio correcto
npm install
npm run build
sudo cp -r dist/* /var/www/gea-front/   # o la ruta real que sirva Apache/Nginx
```
No hace falta reiniciar Apache para esto — los archivos estáticos se sirven
directo desde el disco.

## 8. Diagnóstico general del servidor

```bash
# Espacio en disco (revisar que ninguna partición esté casi llena)
df -h

# Memoria RAM disponible
free -h

# Qué proceso está usando más CPU/RAM en este momento
top

# Confirmar que el backend está escuchando en su puerto
sudo ss -tulpn | grep 8083

# Confirmar que MariaDB está escuchando (normalmente solo en localhost)
sudo ss -tulpn | grep 3306

# Ver reglas de firewall activas
sudo ufw status
```

## 9. Dónde mirar si algo falla

| Componente | Dónde ver |
|---|---|
| Backend | `sudo journalctl -u gea-backend -f` |
| Apache | `/opt/lampp/logs/error_log`, `/opt/lampp/logs/access_log` |
| MariaDB | `/opt/lampp/var/mysql/*.err` |
| Backups | `/var/log/gea-backup.log` |


