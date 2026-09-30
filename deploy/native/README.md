# Despliegue nativo (sin Docker) — Ubuntu/Debian

Guía para levantar GEA en un servidor Linux que **ya tiene Java 21, MySQL 8 y
Nginx instalados**, sin contenedores. Si el servidor sí puede usar Docker,
usar en cambio el `docker-compose.yml` de la raíz del repo (ver sección
"Despliegue con Docker" del `README.md` principal) — es más simple de
mantener y actualizar.

Componentes:
- **MySQL**: nativo, tuning en `mysql-gea.cnf`.
- **Backend**: JAR corrido como servicio de `systemd` (`gea-backend.service`).
- **Frontend**: build estático de Vite servido directo por Nginx nativo
  (`../../../GEA_FRONT/deploy/nginx-gea.conf`, en el otro repo).

## Vía rápida: `install.sh`

Los pasos 1, 3, 4, 5 y 6 de abajo (usuario del sistema, compilar y arrancar
el backend, compilar el frontend, instalar Nginx, firewall) están
automatizados en un solo script. **La base de datos (paso 2) NO la toca —
eso se hace a mano primero**, con el mismo nombre de base, usuario y
contraseña que ya están en `.env`. Requiere que ya estén clonados
ambos repos como carpetas hermanas y que ya tengas el archivo de variables
reales (ej. `backend.env.real`) copiado como `.env` en la raíz de
este repo — **formato nativo** (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`...), no confundir con el `.env` de `docker-compose.yml`
(`DB_ROOT_PASSWORD`, `DB_NAME`) si alguna vez se usa Docker en este mismo repo:

```bash
cd GEA_BACKEND
cp backend.env.real .env   # el archivo con los valores reales que te pasaron

# Primero, a mano: crear la base de datos y el usuario (ver paso 2 más abajo)
# e importar el dump si aplica.

sudo ./deploy/native/install.sh
```

Es seguro volver a correrlo si algo falla a mitad de camino. Si prefieres
entender o ejecutar cada paso a mano (por ejemplo para adaptar algo puntual),
sigue la guía completa más abajo — es exactamente lo que el script automatiza.

**PWA de la app móvil (opcional):** el script no publica la PWA (repo
`gea_app` compilado como Flutter Web, servida bajo `/app/`) a menos que se
pida explícitamente, porque requiere el SDK de Flutter instalado en el
servidor — algo que no todo despliegue necesita:

```bash
sudo DEPLOY_PWA=true ./deploy/native/install.sh
```

Asume `gea_app` como carpeta hermana (`../gea_app`); usa
`PWA_REPO_PATH=/ruta/a/gea_app` si no es el caso.

**Servidor con Apache ya ocupando 80/443:** si el servidor de producción
tiene un Apache que sirve otra aplicación y no se puede apagar, este modo
pone a Apache de cara al público (termina el TLS ahí, con el certificado
real) y Nginx pasa a escuchar solo en `127.0.0.1:8080` — invisible desde
afuera, sin que la URL final lleve puerto. Requiere Apache ya instalado
(este script no lo instala) y el certificado ya copiado en
`/etc/nginx/certs/` (mismo lugar que usa el modo TLS normal).

**Se activa solo, automáticamente, cuando `COOKIE_SECURE=true` en `.env`**
(la señal de "esto es producción con TLS real") — en este proyecto Apache
siempre está presente en producción, así que no hace falta pasar la
variable a mano. Para forzarlo en cualquier sentido:

```bash
sudo DEPLOY_BEHIND_APACHE=true  ./deploy/native/install.sh   # forzar activado
sudo DEPLOY_BEHIND_APACHE=false ./deploy/native/install.sh   # forzar apagado
```

Ver `deploy/apache-gea-proxy.conf` y
`../../../GEA_FRONT/deploy/nginx-gea-tras-apache.conf` (en el otro repo)
para el detalle de por qué esa variante de Nginx reenvía los headers
`X-Forwarded-*` en vez de generarlos — es necesario para que el
rate-limiter del backend siga viendo la IP real del usuario y no la de
Apache.

---

## 1. Usuario y carpetas del sistema

```bash
sudo useradd --system --home /opt/gea/backend --shell /usr/sbin/nologin gea
sudo mkdir -p /opt/gea/backend/uploads /etc/gea
sudo chown -R gea:gea /opt/gea/backend
```

## 2. Base de datos

Crear la base de datos y un usuario dedicado para la app (NO usar root):

En Ubuntu/Debian, el root de MySQL usa `auth_socket` por defecto — como ya
estás como root (`sudo`), conecta sin contraseña. (Si tu servidor tiene una
contraseña real puesta para root, usa `mysql -u root -p` en su lugar; evita
combinar `-p` interactivo con un heredoc si no hay una terminal real
disponible — puede saltarse el SQL en silencio sin avisar del error.)

```bash
sudo mysql -u root <<'SQL'
CREATE DATABASE gea CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'gea_app'@'localhost' IDENTIFIED BY 'CAMBIAR_POR_UNA_CONTRASENA_FUERTE';
-- Privilegios de datos + los que necesita mysqldump --single-transaction --routines --triggers --events
-- CREATE VIEW es obligatorio: el esquema baseline crea vistas
-- (ej. v_anuncios_publicados) y sin este privilegio Flyway falla al migrar.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES,
      LOCK TABLES, CREATE VIEW, SHOW VIEW, TRIGGER, EVENT, EXECUTE, CREATE ROUTINE, ALTER ROUTINE
      ON gea.* TO 'gea_app'@'localhost';
FLUSH PRIVILEGES;
SQL
```

Aplicar el tuning (ajustar RAM según la tabla del plan de migración antes de copiar):

```bash
sudo cp deploy/native/mysql-gea.cnf /etc/mysql/mysql.conf.d/gea.cnf
sudo systemctl restart mysql
```

MySQL en Ubuntu ya escucha solo en `127.0.0.1` por defecto
(`bind-address` en `/etc/mysql/mysql.conf.d/mysqld.cnf`) — confirmar que
sigue así, no debe ser alcanzable desde fuera del servidor.

## 3. Backend

Compilar el JAR (en el propio servidor o en una máquina de build, y copiar el
resultado):

```bash
./mvnw clean package -DskipTests
sudo cp target/callapp_backend-0.0.1-SNAPSHOT.jar /opt/gea/backend/app.jar
sudo chown gea:gea /opt/gea/backend/app.jar
```

Configurar las variables reales:

```bash
sudo cp deploy/native/.env.example /etc/gea/backend.env
sudo nano /etc/gea/backend.env   # completar cada CAMBIAR_POR_...
sudo chmod 600 /etc/gea/backend.env
sudo chown gea:gea /etc/gea/backend.env
```

Instalar y arrancar el servicio:

```bash
sudo cp deploy/native/gea-backend.service /etc/systemd/system/gea-backend.service
sudo systemctl daemon-reload
sudo systemctl enable --now gea-backend
sudo systemctl status gea-backend
```

Verificar que arrancó bien (Flyway debe migrar el esquema en el primer arranque):

```bash
journalctl -u gea-backend -f
curl http://127.0.0.1:8084/actuator/health
```
(Actuator corre en el puerto de gestión separado `8084`, no en el `8083` de la
API, y sin el prefijo `/api` — ver `management.server.port` en
`application.properties`.)

## 4. Frontend

Compilar el build de producción y copiarlo a donde Nginx lo sirve:

```bash
cd ../GEA_FRONT   # ajustar a donde esté clonado
nano .env.production   # confirmar VITE_API_URL = dominio público real
npm install
npm run build
sudo mkdir -p /var/www/gea-front
sudo cp -r dist/* /var/www/gea-front/
```

Instalar la config de Nginx:

```bash
sudo cp deploy/nginx-gea.conf /etc/nginx/sites-available/gea
sudo ln -s /etc/nginx/sites-available/gea /etc/nginx/sites-enabled/gea
sudo rm -f /etc/nginx/sites-enabled/default
```

Certificado TLS — con Let's Encrypt si el servidor es alcanzable públicamente:

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d gea.tudominio.edu.co
```

(certbot reescribe automáticamente las rutas `ssl_certificate` del archivo).
Si en cambio es la CA interna de la universidad, copiar `fullchain.pem` y
`privkey.pem` reales a `/etc/nginx/certs/` (rutas que ya tiene el archivo por
defecto).

```bash
sudo nginx -t && sudo systemctl reload nginx
```

## 5. Firewall

Solo 80/443 deben ser alcanzables desde fuera del servidor. MySQL (3306) y
el backend (8083) deben quedar solo en `localhost`:

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow OpenSSH
sudo ufw enable
```

## 6. Backups

```bash
sudo mkdir -p /backups
sudo cp deploy/native/backup-native.sh /opt/gea/backup-native.sh
sudo chmod +x /opt/gea/backup-native.sh
# Cron diario a las 2am, con la contraseña del usuario gea_app creado en el paso 2
echo '0 2 * * * DB_PASSWORD=LA_MISMA_CONTRASENA_DE_GEA_APP /opt/gea/backup-native.sh >> /var/log/gea-backup.log 2>&1' | sudo tee /etc/cron.d/gea-backup
```

## 7. Actualizar a una nueva versión

```bash
git pull
./mvnw clean package -DskipTests
sudo systemctl stop gea-backend
sudo cp target/callapp_backend-0.0.1-SNAPSHOT.jar /opt/gea/backend/app.jar
sudo systemctl start gea-backend
```

Para el frontend: `npm run build` de nuevo y volver a copiar `dist/*` a
`/var/www/gea-front/` (no requiere reiniciar Nginx).

## 8. Dónde mirar si algo falla

| Componente | Logs |
|---|---|
| Backend | `journalctl -u gea-backend -f` |
| Nginx | `/var/log/nginx/error.log`, `/var/log/nginx/access.log` |
| MySQL (slow queries) | `/var/log/mysql/slow.log` |
| Backup | `/var/log/gea-backup.log` |
