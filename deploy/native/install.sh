#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# GEA — instalación nativa automatizada (sin Docker)
#
# Uso (desde la raíz de GEA_BACKEND):
#   sudo ./deploy/native/install.sh
#
# Nginx y Node 20+ se instalan solos si faltan o si la versión que ya está
# instalada no alcanza (ver paso 0/8). Java SÍ se verifica (21+) pero no se
# instala — falla acá mismo con un mensaje claro si falta. MariaDB/MySQL no
# se verifica en absoluto (se asume ya preparado); si falta o las
# credenciales no coinciden, el backend arranca mal y el script para en el
# paso 4/8. El paso 0 también revisa que haya salida a internet antes de
# arrancar (varios pasos más adelante necesitan descargar cosas).
#
# Este script NO crea la base de datos, el usuario ni importa ningún dump
# — eso se hace a mano antes de correrlo (ver deploy/native/README.md,
# sección "Base de datos"), usando exactamente el mismo nombre de base,
# usuario y contraseña que ya están en .env.
#
# Lo único que sí hay que preparar antes:
#   - Java 21 y MariaDB/MySQL ya instalados en el servidor
#   - La base de datos, el usuario y (si aplica) el dump ya importados a
#     mano, con las mismas credenciales que hay en .env
#   - El archivo con los valores reales ya copiado a la raíz de este repo
#     como ".env" (o indicar otra ruta con BACKEND_ENV_FILE=...). OJO: este
#     ".env" es del despliegue NATIVO (DB_URL, DB_USERNAME, DB_PASSWORD,
#     JWT_SECRET...) — NO es el mismo formato que usa docker-compose.yml
#     (DB_ROOT_PASSWORD, DB_NAME); si algún día se usa Docker en este mismo
#     repo, no reutilizar el mismo archivo entre ambos despliegues.
#   - El repo GEA_FRONT en /opt/lampp/htdocs/gea/GEA_FRONT-main (ruta real
#     del servidor de producción), a menos que se indique otra con
#     FRONTEND_REPO_PATH=...
#
# PWA de la app móvil (repo gea_app, build de Flutter Web bajo /app/): es
# OPCIONAL y está apagada por defecto porque necesita el SDK de Flutter
# completo instalado en el servidor, algo que no todo despliegue necesita.
# Para activarla:
#   sudo DEPLOY_PWA=true ./deploy/native/install.sh
# Asume el repo gea_app como carpeta hermana (../gea_app); para otra ruta:
#   sudo DEPLOY_PWA=true PWA_REPO_PATH=/ruta/a/gea_app ./deploy/native/install.sh
# El Flutter SDK, igual que Java/MariaDB, NO se instala solo — se asume ya
# presente (flutter en el PATH); si falta, el script falla con un mensaje
# claro en vez de intentar instalarlo.
#
# Servidor de producción con Apache ya ocupando 80/443 (y que no se puede
# apagar porque sirve otra aplicación): activa el modo "puente" — Apache
# termina TLS en 80/443 con el certificado real y reenvía todo hacia
# Nginx, que pasa a escuchar solo en 127.0.0.1:8080 (invisible desde
# afuera, el usuario nunca ve ese puerto). Requiere Apache YA instalado
# (igual que Java/MariaDB, este script no lo instala) y el certificado en
# /etc/nginx/certs/ (mismo lugar que ya usa el modo TLS normal). Ver
# deploy/apache-gea-proxy.conf y deploy/nginx-gea-tras-apache.conf para el
# detalle de por qué esta variante de Nginx reenvía los headers
# X-Forwarded-* en vez de generarlos — es necesario para que el
# rate-limiter del backend siga viendo la IP real del usuario y no la de
# Apache.
#
# AUTOMÁTICO desde COOKIE_SECURE: en este proyecto, Apache siempre está
# presente en el servidor de producción (nunca en el de pruebas), así que
# DEPLOY_BEHIND_APACHE se activa solo cuando COOKIE_SECURE=true en .env —
# no hace falta pasarlo a mano. Para forzarlo explícitamente en cualquier
# sentido (por ejemplo, si algún día el de pruebas también tiene Apache
# con certificado real):
#   sudo DEPLOY_BEHIND_APACHE=true  ./deploy/native/install.sh
#   sudo DEPLOY_BEHIND_APACHE=false ./deploy/native/install.sh
#
# Qué hace, en orden: crea el usuario/carpetas del sistema, compila e
# instala el backend como servicio systemd, compila el frontend y lo copia
# a Nginx, compila la PWA (solo si DEPLOY_PWA=true) y la copia a Nginx,
# instala la config de Nginx (según COOKIE_SECURE en .env, o el
# modo puente si DEPLOY_BEHIND_APACHE=true) y configura el firewall.
#
# Es seguro volver a correrlo si algo falla a mitad de camino — los pasos
# están hechos para no duplicar nada si ya se corrieron antes.
# ============================================================================

BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-.env}"
# Personalizado para la estructura de carpetas real del servidor de
# producción (XAMPP): FRONTEND_REPO_PATH se puede seguir pisando con la
# variable de entorno si algún día cambia de lugar.
FRONTEND_REPO_PATH="${FRONTEND_REPO_PATH:-/opt/lampp/htdocs/gea/GEA_FRONT-main}"
DEPLOY_PWA="${DEPLOY_PWA:-false}"
PWA_REPO_PATH="${PWA_REPO_PATH:-../gea_app}"
# Sin valor por defecto fijo a propósito: "__auto__" es un centinela que más
# abajo (ya con COOKIE_SECURE_VAL leído de .env) se resuelve a true cuando
# COOKIE_SECURE=true (producción — ahí Apache siempre está presente) y a
# false en pruebas (sin Apache de por medio). Si se pasa explícitamente
# DEPLOY_BEHIND_APACHE=true/false, eso manda sobre el auto-detectado.
DEPLOY_BEHIND_APACHE="${DEPLOY_BEHIND_APACHE:-__auto__}"

echo "== 0/8: verificaciones previas =="

if [[ $EUID -ne 0 ]]; then
  echo "Este script necesita sudo/root. Corre: sudo $0 $*" >&2
  exit 1
fi

# Varios pasos más adelante necesitan internet (apt, NodeSource, Maven, npm,
# y Flutter si se usa la PWA) — cada uno falla con un error nativo distinto
# y críptico si no hay conexión (confirmado con una corrida real: el error
# de apt-get no se parece en nada al de npm). Se revisa acá, una sola vez,
# con un mensaje claro, en vez de dejar que cada paso falle por su cuenta
# más adelante. Prueba conexión TCP directa a IPs conocidas (no nombres de
# dominio) para no depender de que el DNS también esté funcionando —
# suficiente para confirmar "hay salida a internet", no busca diagnosticar
# el motivo exacto si no la hay.
if [[ "${SKIP_INTERNET_CHECK:-false}" != "true" ]]; then
  internet_ok=false
  for target in 1.1.1.1:443 8.8.8.8:443; do
    host="${target%%:*}"
    port="${target##*:}"
    if timeout 3 bash -c "cat < /dev/null > /dev/tcp/${host}/${port}" 2>/dev/null; then
      internet_ok=true
      break
    fi
  done
  if [[ "$internet_ok" != "true" ]]; then
    echo "No se detecta salida a internet desde este servidor." >&2
    echo "" >&2
    echo "Revisa la conexión de red de este servidor y vuelve a correr el script." >&2
    echo "" >&2
    echo "Si ya tienes todo cacheado de antes (paquetes de apt, ~/.m2 de Maven," >&2
    echo "node_modules) y de verdad no hace falta internet esta vez, salta este" >&2
    echo "chequeo con:" >&2
    echo "  sudo SKIP_INTERNET_CHECK=true ./deploy/native/install.sh" >&2
    exit 1
  fi
fi

# Instala automáticamente lo que falte (Ubuntu/Debian, vía apt) en vez de
# solo revisar y fallar — así no hay que preparar el servidor a mano antes.
# systemd no está en esta lista porque ya viene con cualquier Ubuntu/Debian
# moderno (no se instala aparte); lo que sí configura este script es el
# SERVICIO systemd del backend (ver paso 4/8 más abajo).
#
# A propósito, este script SOLO instala Nginx y Node/npm. Java SÍ se
# verifica (versión 21+) pero no se instala — falla acá mismo con un
# mensaje claro si falta o es muy vieja, en vez de fallar profundo dentro
# de "mvnw" con un error de bytecode incompatible. MariaDB/MySQL no se
# verifica (no hay una forma simple de version-checkearlo desde acá); si
# falta o las credenciales no coinciden, el backend arranca mal y el
# script para en el paso 4/8 con ese diagnóstico.
declare -A PKG_FOR_CMD=(
  [nginx]=nginx
  [curl]=curl
)

# curl se instala igual aunque no se haya pedido explícitamente — el propio
# script lo necesita internamente (health-check del backend, descargar el
# script de NodeSource para Node), no es un prerrequisito de la app.
missing_pkgs=()
for cmd in nginx curl; do
  command -v "$cmd" >/dev/null 2>&1 || missing_pkgs+=("${PKG_FOR_CMD[$cmd]}")
done

# Para node no basta con que el comando exista — si ya hay una versión
# instalada pero más vieja de lo que el proyecto necesita, usarla tal cual
# rompe más adelante (confirmado con una corrida real): Vite 8 exige Node
# 20.19+/22.12+ (con Node 18, "npm run build" truena con "ReferenceError:
# CustomEvent is not defined").
need_node=false
if command -v npm >/dev/null 2>&1 && command -v node >/dev/null 2>&1; then
  node_major=$(node -v | sed -E 's/^v([0-9]+)\..*/\1/')
  node_minor=$(node -v | sed -E 's/^v[0-9]+\.([0-9]+)\..*/\1/')
  if [[ ! "$node_major" =~ ^[0-9]+$ ]] || [[ "$node_major" -lt 20 ]] || { [[ "$node_major" -eq 20 ]] && [[ "$node_minor" -lt 19 ]]; }; then
    need_node=true
  fi
else
  need_node=true
fi

if [[ ${#missing_pkgs[@]} -gt 0 || "$need_node" == true ]]; then
  if ! command -v apt-get >/dev/null 2>&1; then
    echo "Falta software y este servidor no usa apt (¿no es Ubuntu/Debian?): ${missing_pkgs[*]}" >&2
    echo "Instálalos manualmente con el gestor de paquetes de tu distro y vuelve a correr el script." >&2
    exit 1
  fi
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  if [[ ${#missing_pkgs[@]} -gt 0 ]]; then
    echo "Instalando paquetes faltantes: ${missing_pkgs[*]}"
    apt-get install -y -qq "${missing_pkgs[@]}"
  fi
  if [[ "$need_node" == true ]]; then
    # El "npm" de los repos de Ubuntu 24.04 trae Node.js 18, y Vite 8 exige
    # Node 20.19+/22.12+ — con Node 18 el build del frontend truena con
    # "ReferenceError: CustomEvent is not defined" (confirmado con una
    # corrida real). Se instala Node 20 vía NodeSource en su lugar.
    echo "Instalando Node.js 20 (vía NodeSource, para cumplir el mínimo de Vite)..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - >/dev/null 2>&1
    apt-get install -y -qq nodejs
  fi
  systemctl enable --now nginx 2>/dev/null || true
fi

# Java SÍ se verifica (a diferencia de MariaDB, que no hay forma sencilla de
# version-checkear desde acá) pero NO se instala — mismo criterio que
# Java/MariaDB en general: se asumen preparados de antemano. La diferencia
# es que antes esto fallaba profundo dentro de "mvnw" con un error de
# bytecode incompatible, difícil de diagnosticar; ahora falla acá mismo,
# con un mensaje claro, antes de gastar tiempo en cualquier otro paso.
if ! command -v java >/dev/null 2>&1; then
  echo "No se encontró 'java' en este servidor — este proyecto necesita Java 21." >&2
  echo "Instálalo (este script no lo hace por ti) y vuelve a correr el script." >&2
  exit 1
fi
java_major=$(java -version 2>&1 | head -n1 | grep -oE '"[0-9]+' | tr -d '"')
if [[ ! "$java_major" =~ ^[0-9]+$ ]] || [[ "$java_major" -lt 21 ]]; then
  echo "Se encontró Java pero es la versión ${java_major:-desconocida} — este proyecto necesita Java 21+." >&2
  echo "Instala Java 21 (puede coexistir con otras versiones vía update-alternatives) y vuelve a correr." >&2
  exit 1
fi

if [[ ! -f "$BACKEND_ENV_FILE" ]]; then
  echo "No se encontró '$BACKEND_ENV_FILE' en el directorio actual." >&2
  echo "Copia primero el archivo con los valores reales (ej. backend.env.real) como '$BACKEND_ENV_FILE' en la raíz de GEA_BACKEND." >&2
  echo "(Formato nativo: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET... — NO el .env de docker-compose.)" >&2
  exit 1
fi
# Resolver a ruta absoluta ahora — más adelante el script hace "pushd" a la
# carpeta del frontend, y una ruta relativa dejaría de apuntar al archivo
# correcto en ese punto.
BACKEND_ENV_FILE="$(realpath "$BACKEND_ENV_FILE")"

# Señal única para elegir el dart_define de la PWA, la config de Nginx
# (con/sin TLS) y ahora también si se activa el modo puente de Apache —
# evita depender de que alguien recuerde mantener las tres decisiones en
# sincronía al pasar de pruebas a producción.
COOKIE_SECURE_VAL=$(grep -E '^COOKIE_SECURE=' "$BACKEND_ENV_FILE" | cut -d '=' -f2- || echo "false")

if [[ "$DEPLOY_BEHIND_APACHE" == "__auto__" ]]; then
  if [[ "$COOKIE_SECURE_VAL" == "true" ]]; then
    DEPLOY_BEHIND_APACHE=true
    echo "COOKIE_SECURE=true detectado — DEPLOY_BEHIND_APACHE=true automático (producción, Apache al frente)."
  else
    DEPLOY_BEHIND_APACHE=false
  fi
fi

if [[ ! -d "$FRONTEND_REPO_PATH" ]]; then
  echo "No se encontró el repo del frontend en '$FRONTEND_REPO_PATH'." >&2
  echo "Ajusta FRONTEND_REPO_PATH si tu estructura de carpetas es distinta, ej.:" >&2
  echo "  sudo FRONTEND_REPO_PATH=/ruta/a/GEA_FRONT ./deploy/native/install.sh" >&2
  exit 1
fi

if [[ "$DEPLOY_PWA" == "true" ]]; then
  if [[ ! -d "$PWA_REPO_PATH" ]]; then
    echo "DEPLOY_PWA=true pero no se encontró el repo gea_app en '$PWA_REPO_PATH'." >&2
    echo "Ajusta PWA_REPO_PATH si tu estructura de carpetas es distinta, ej.:" >&2
    echo "  sudo DEPLOY_PWA=true PWA_REPO_PATH=/ruta/a/gea_app ./deploy/native/install.sh" >&2
    exit 1
  fi
  # Igual que BACKEND_ENV_FILE: resolver a ruta absoluta ahora, porque el
  # paso de la PWA hace "pushd" anidado dentro del "pushd" del frontend, y
  # una ruta relativa (ej. "../gea_app") dejaría de apuntar al lugar
  # correcto una vez el cwd ya cambió.
  PWA_REPO_PATH="$(realpath "$PWA_REPO_PATH")"
  if ! command -v flutter >/dev/null 2>&1; then
    echo "DEPLOY_PWA=true pero no se encontró 'flutter' en el PATH." >&2
    echo "Instala el Flutter SDK en este servidor y vuelve a correr el script." >&2
    exit 1
  fi
fi

if [[ "$DEPLOY_BEHIND_APACHE" == "true" ]]; then
  # Detecta cuál de los dos "sabores" de Apache hay instalados — XAMPP
  # (común en servidores donde alguien más ya tenía un stack propio antes
  # de GEA) y el paquete "apache2" de Debian/Ubuntu no se manejan igual en
  # absoluto: XAMPP no tiene a2enmod/a2ensite/sites-available, ni corre
  # como un "apache2.service" normal de systemd — se controla con
  # /opt/lampp/lampp. Confirmado con una corrida real.
  if [[ -x /opt/lampp/lampp ]]; then
    APACHE_FLAVOR="xampp"
  elif command -v a2enmod >/dev/null 2>&1 && command -v apache2ctl >/dev/null 2>&1; then
    APACHE_FLAVOR="debian"
  else
    echo "DEPLOY_BEHIND_APACHE quedó activado (a mano, o automático porque COOKIE_SECURE=true en .env)" >&2
    echo "pero no se encontró Apache en este servidor (ni XAMPP en /opt/lampp, ni el paquete" >&2
    echo "apache2 de Debian/Ubuntu con a2enmod/apache2ctl)." >&2
    echo "Este modo asume que Apache ya está instalado y en uso — instálalo primero si de verdad lo necesitas," >&2
    echo "o fuerza DEPLOY_BEHIND_APACHE=false si este servidor no usa Apache." >&2
    exit 1
  fi
  if [[ ! -f /etc/nginx/certs/fullchain.pem || ! -f /etc/nginx/certs/privkey.pem ]]; then
    echo "DEPLOY_BEHIND_APACHE quedó activado (a mano, o automático porque COOKIE_SECURE=true en .env)" >&2
    echo "pero no se encontraron los certificados en /etc/nginx/certs/." >&2
    echo "Genera uno real primero (ej. 'sudo certbot --apache -d tu-dominio.edu.co')" >&2
    echo "o copia fullchain.pem/privkey.pem ahí, y vuelve a correr el script." >&2
    exit 1
  fi
fi

if [[ "$DEPLOY_PWA" == "true" ]]; then
  if [[ "$COOKIE_SECURE_VAL" == "true" ]]; then
    PWA_DART_DEFINE="dart_define.produccion.json"
  else
    PWA_DART_DEFINE="dart_define.pruebas.json"
  fi
  # Se valida aquí, antes de compilar nada, para no gastar tiempo
  # compilando backend y frontend si al final va a fallar por esto.
  if [[ ! -f "$PWA_REPO_PATH/$PWA_DART_DEFINE" ]]; then
    echo "DEPLOY_PWA=true pero no se encontró '$PWA_DART_DEFINE' en $PWA_REPO_PATH." >&2
    echo "Copia la plantilla correspondiente (${PWA_DART_DEFINE%.json}.example.json) y completa los valores reales." >&2
    exit 1
  fi
fi

echo "== 1/8: usuario y carpetas del sistema =="
id -u gea >/dev/null 2>&1 || useradd --system --home /opt/gea/backend --shell /usr/sbin/nologin gea
mkdir -p /opt/gea/backend/uploads /etc/gea
chown -R gea:gea /opt/gea/backend

echo "== 2/8: compilar backend =="
./mvnw clean package -DskipTests -q
cp target/callapp_backend-*.jar /opt/gea/backend/app.jar
chown gea:gea /opt/gea/backend/app.jar

echo "== 3/8: variables de entorno del backend =="
cp "$BACKEND_ENV_FILE" /etc/gea/backend.env
chmod 600 /etc/gea/backend.env
chown gea:gea /etc/gea/backend.env

echo "== 4/8: servicio systemd =="
cp deploy/native/gea-backend.service /etc/systemd/system/gea-backend.service
systemctl daemon-reload
systemctl enable --now gea-backend

echo "Esperando a que el backend arranque..."
backend_up=false
for i in $(seq 1 30); do
  if curl -sf http://127.0.0.1:8084/actuator/health >/dev/null 2>&1; then
    echo "Backend arriba."
    backend_up=true
    break
  fi
  sleep 2
done
if [[ "$backend_up" != "true" ]]; then
  echo "El backend no respondió a tiempo (60s) — no tiene sentido seguir instalando" >&2
  echo "Nginx/firewall encima de un backend caído, así que el script para acá." >&2
  echo "" >&2
  echo "Revisa la causa exacta con: journalctl -u gea-backend -e --no-pager" >&2
  echo "" >&2
  echo "Causas típicas:" >&2
  echo "  - La base de datos no existe, o el usuario/contraseña en .env no coinciden" >&2
  echo "    con los que de verdad tiene MariaDB/MySQL (ver deploy/native/README.md," >&2
  echo "    sección 'Base de datos')." >&2
  echo "  - El puerto 8083 ya estaba ocupado por otro proceso." >&2
  echo "  - .env tiene algún valor mal escrito (JWT_SECRET vacío, DB_URL con typo, etc.)." >&2
  echo "" >&2
  echo "Corrige el problema y vuelve a correr el script — es seguro repetirlo, no" >&2
  echo "duplica nada de lo que ya se instaló." >&2
  exit 1
fi

echo "== 5/8: frontend =="
pushd "$FRONTEND_REPO_PATH" >/dev/null
npm install
npm run build
mkdir -p /var/www/gea-front
cp -r dist/* /var/www/gea-front/

echo "== 6/8: PWA (app móvil) =="
if [[ "$DEPLOY_PWA" == "true" ]]; then
  pushd "$PWA_REPO_PATH" >/dev/null
  flutter build web --release --base-href /app/ --dart-define-from-file="$PWA_DART_DEFINE"
  mkdir -p /var/www/gea-app-pwa
  cp -r build/web/* /var/www/gea-app-pwa/
  popd >/dev/null
else
  echo "DEPLOY_PWA=false — se omite (usa DEPLOY_PWA=true para publicar la PWA en /app/)."
fi

echo "== 7/8: Nginx =="
# La directiva proxy_cache_path de ambas configs necesita que esta carpeta
# ya exista — nginx no la crea sola, y en una instalación nueva de Nginx
# /var/cache/nginx/ puede no existir todavía, lo que hace fallar el
# "nginx -t" con "mkdir() ... failed (2: No such file or directory)".
mkdir -p /var/cache/nginx/gea_public
chown -R www-data:www-data /var/cache/nginx

# Elige la config de Nginx: primero si hay que usar el modo "puente"
# detrás de Apache (DEPLOY_BEHIND_APACHE), si no según COOKIE_SECURE en
# .env — la misma señal que ya decide si el backend espera HTTPS
# real (true) o no (false, servidor de pruebas sin dominio/certificado).
# Evita depender de que alguien recuerde cambiar esto a mano al pasar de
# pruebas a producción.
if [[ "$DEPLOY_BEHIND_APACHE" == "true" ]]; then
  NGINX_CONF="deploy/nginx-gea-tras-apache.conf"
  echo "DEPLOY_BEHIND_APACHE=true — usando $NGINX_CONF (Nginx solo en 127.0.0.1:8080, Apache al frente)."
elif [[ "$COOKIE_SECURE_VAL" == "true" ]]; then
  NGINX_CONF="deploy/nginx-gea.conf"
  echo "COOKIE_SECURE=true detectado — usando $NGINX_CONF (con TLS)."
  if [[ ! -f /etc/nginx/certs/fullchain.pem || ! -f /etc/nginx/certs/privkey.pem ]]; then
    echo "AVISO: no se encontraron los certificados en /etc/nginx/certs/." >&2
    echo "Genera uno real primero (ej. 'sudo certbot --nginx -d tu-dominio.edu.co')" >&2
    echo "o copia fullchain.pem/privkey.pem ahí, y vuelve a correr el script." >&2
    exit 1
  fi
else
  NGINX_CONF="deploy/nginx-gea-http-pruebas.conf"
  echo "COOKIE_SECURE=false detectado — usando $NGINX_CONF (sin TLS, servidor de pruebas)."
fi

cp "$NGINX_CONF" /etc/nginx/sites-available/gea
ln -sf /etc/nginx/sites-available/gea /etc/nginx/sites-enabled/gea
rm -f /etc/nginx/sites-enabled/default
nginx -t
# "reload" a secas falla duro ("nginx.service is not active, cannot
# reload") si Nginx nunca llegó a arrancar la primera vez — algo que pasa
# de verdad cuando el paquete se instaló con el puerto 80 ya ocupado (por
# Apache/XAMPP) y el propio postinst de nginx decidió no arrancarlo solo.
# "reload-or-restart" cubre los dos casos: si ya está corriendo, recarga
# sin cortar conexiones; si no, arranca de cero.
systemctl reload-or-restart nginx

# "nginx -t" solo valida la sintaxis, y "reload" puede reportar éxito
# (el systemd unit respondió bien a la señal) aunque el worker nuevo no
# haya logrado bindear el puerto de verdad — por ejemplo si algo más (un
# Apache sin DEPLOY_BEHIND_APACHE) ya lo tiene ocupado. Se verifica acá
# mismo, conectando de verdad al puerto que este .conf dice usar, en vez
# de asumir que "reload sin error" significa "está sirviendo".
NGINX_OWN_PORTS=$(grep -oE 'listen +([0-9.]+:)?[0-9]+' /etc/nginx/sites-available/gea | grep -oE '[0-9]+$' | sort -un)
sleep 1
for port in $NGINX_OWN_PORTS; do
  if ! timeout 2 bash -c "cat < /dev/null > /dev/tcp/127.0.0.1/${port}" 2>/dev/null; then
    echo "Nginx no quedó escuchando en el puerto ${port} después del reload," >&2
    echo "aunque la config sea sintácticamente válida." >&2
    echo "Revisa qué más está usando ese puerto: ss -ltnp | grep :${port}" >&2
    echo "(causa típica: un Apache u otro servicio ya lo tiene ocupado — si es Apache" >&2
    echo "de producción, activa DEPLOY_BEHIND_APACHE=true en vez de compartir el puerto)." >&2
    exit 1
  fi
done

if [[ "$DEPLOY_BEHIND_APACHE" == "true" ]]; then
  echo "Configurando Apache como puente hacia Nginx ($APACHE_FLAVOR)..."

  if [[ "$APACHE_FLAVOR" == "xampp" ]]; then
    # XAMPP no tiene a2enmod: los módulos (proxy, proxy_http, ssl, rewrite,
    # headers) se asumen ya habilitados en /opt/lampp/etc/httpd.conf — si
    # falta alguno, "httpd -t" lo va a decir explícitamente más abajo.
    mkdir -p /opt/lampp/etc/extra
    cp deploy/apache-gea-proxy-xampp.conf /opt/lampp/etc/extra/gea-proxy.conf
    # Include propio y separado del httpd-vhosts.conf que XAMPP ya trae
    # (que puede tener otras cosas configuradas) — idempotente: si esta
    # línea ya está de una corrida anterior, no se duplica.
    grep -qxF 'Include etc/extra/gea-proxy.conf' /opt/lampp/etc/httpd.conf || \
      echo 'Include etc/extra/gea-proxy.conf' >> /opt/lampp/etc/httpd.conf
    /opt/lampp/bin/httpd -t
    # XAMPP no tiene una recarga en caliente equivalente a "systemctl
    # reload" — restartapache para (stop+start) y vuelve a levantar Apache
    # directo, sin pasar por el "xampp.service" de systemd (confirmado en
    # este servidor que ese unit está mal configurado — no arranca nada de
    # verdad, solo reinicia en loop).
    /opt/lampp/lampp restartapache
  else
    a2enmod proxy proxy_http ssl headers rewrite >/dev/null
    cp deploy/apache-gea-proxy.conf /etc/apache2/sites-available/gea.conf
    a2ensite gea.conf >/dev/null
    apache2ctl configtest
    systemctl reload apache2
  fi

  # "reload"/"restartapache" no siempre alcanza a abrir un puerto que
  # Apache nunca había escuchado antes (ej. primera vez que este servidor
  # sirve HTTPS) — si el 443 no quedó escuchando, se hace un restart real
  # como respaldo (en XAMPP, restartapache YA es un restart completo, así
  # que ahí este segundo intento es solo por si el primero fue demasiado
  # rápido para que el puerto quedara arriba).
  sleep 1
  if ! timeout 2 bash -c "cat < /dev/null > /dev/tcp/127.0.0.1/443" 2>/dev/null; then
    echo "AVISO: el puerto 443 no quedó escuchando tras configurar Apache" >&2
    echo "(probablemente es la primera vez que este servidor sirve HTTPS)." >&2
    if [[ "$APACHE_FLAVOR" == "xampp" ]]; then
      echo "Reintentando con 'lampp restartapache'..." >&2
      /opt/lampp/lampp restartapache
    else
      echo "Haciendo 'systemctl restart apache2' como respaldo..." >&2
      systemctl restart apache2
    fi
    sleep 1
    if ! timeout 2 bash -c "cat < /dev/null > /dev/tcp/127.0.0.1/443" 2>/dev/null; then
      if [[ "$APACHE_FLAVOR" == "xampp" ]]; then
        echo "El puerto 443 sigue sin responder. Revisa: tail -f /opt/lampp/logs/error_log" >&2
      else
        echo "El puerto 443 sigue sin responder. Revisa: journalctl -u apache2 -e" >&2
      fi
      exit 1
    fi
  fi
fi
popd >/dev/null

# Se leen los puertos directamente del archivo ya instalado (no se asumen
# fijos en 80/443) porque un servidor puede tener otro proceso (ej. un
# Apache ya en uso) ocupando el 80 — en ese caso el .conf de Nginx se edita
# para escuchar en otro puerto (ver comentario en nginx-gea-http-pruebas.conf),
# y el firewall/mensaje final deben reflejar ese cambio automáticamente.
# En el modo puente, el puerto de Nginx (127.0.0.1:8080) es solo local y
# no hay que abrirlo en el firewall — lo que hay que abrir es 80/443, que
# es donde escucha Apache de cara al público.
if [[ "$DEPLOY_BEHIND_APACHE" == "true" ]]; then
  NGINX_PORTS=$'80\n443'
else
  NGINX_PORTS=$(grep -oE 'listen +[0-9]+' /etc/nginx/sites-available/gea | grep -oE '[0-9]+' | sort -un)
fi

echo "== 8/8: firewall =="
if command -v ufw >/dev/null 2>&1; then
  for port in $NGINX_PORTS; do
    ufw allow "${port}/tcp"
  done
  ufw allow OpenSSH
  ufw --force enable
else
  echo "ufw no está instalado en este servidor, se omite la configuración de firewall." >&2
fi

echo ""
echo "Listo. Puerto(s) público(s) activo(s): $(echo "$NGINX_PORTS" | tr '\n' ' ')"
echo "Verifica abriendo http://<IP-del-servidor>:<puerto-de-arriba> en el navegador (sin \":<puerto>\" si es el 80)."
echo "Logs backend: journalctl -u gea-backend -f"
echo "Logs Nginx:   tail -f /var/log/nginx/error.log"
if [[ "$DEPLOY_BEHIND_APACHE" == "true" ]]; then
  if [[ "$APACHE_FLAVOR" == "xampp" ]]; then
    echo "Logs Apache:  tail -f /opt/lampp/logs/error_log"
  else
    echo "Logs Apache:  tail -f /var/log/apache2/error.log"
  fi
fi
