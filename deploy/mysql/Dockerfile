# MySQL con el tuning de producción horneado en la imagen.
#
# No se monta my.cnf como bind-mount porque en Docker Desktop sobre Windows los
# bind-mounts desde el host llegan al contenedor Linux como "world-writable", y
# MySQL ignora por seguridad cualquier archivo de config con esos permisos
# (mysqld: "World-writable config file ... is ignored"). Copiarlo en la imagen y
# fijar el modo explícitamente evita el problema en cualquier host, Windows o Linux.
FROM mysql:8.0
COPY my.cnf /etc/mysql/conf.d/gea.cnf
RUN chmod 644 /etc/mysql/conf.d/gea.cnf
