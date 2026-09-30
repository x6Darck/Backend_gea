# =============================================================================
#  GEA - Exportar base de datos MAESTRA (sin registros transaccionales)
# -----------------------------------------------------------------------------
#  Genera un unico archivo .sql que contiene:
#    * La ESTRUCTURA completa de TODAS las tablas, vistas y triggers.
#    * Los DATOS unicamente de las tablas maestras/catalogo:
#         - roles
#         - oficinas
#         - lugares_fisicos
#         - tipos_evento
#         - usuarios            (conserva logins y contrasenas actuales)
#         - flyway_schema_history  (CRITICO: evita que Flyway reaplique V2..V7)
#
#  NO incluye registros de: solicitudes, publicaciones, participantes,
#  notificaciones, reportes, archivos adjuntos, dispositivos ni historial _aud.
#
#  Uso:
#     powershell -ExecutionPolicy Bypass -File .\scripts\export-db-maestra.ps1
#  El archivo resultante queda en:  scripts\gea_db_maestra.sql
# =============================================================================

# ------------------------- Configuracion (ajustable) -------------------------
$DbHost = "localhost"
$DbPort = "3306"
$DbName = "gea"
$DbUser = "root"
$DbPass = "1234"          # contrasena local de desarrollo

$OutFile = Join-Path $PSScriptRoot "gea_db_maestra.sql"

# Tablas cuyos DATOS se conservan (el resto se exporta solo estructura)
$CatalogTables = @(
    "roles",
    "oficinas",
    "lugares_fisicos",
    "tipos_evento",
    "usuarios",
    "flyway_schema_history",
    "audit_revision_info_seq",   # Hibernate Envers: sin este row -> IdentifierGenerationException
    "revinfo_seq"                # idem para la secuencia de revinfo
)

# ------------------------- Localizar mysqldump -------------------------------
$Candidates = @(
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    "C:\xampp\mysql\bin\mysqldump.exe",
    "mysqldump"
)
$MysqlDump = $null
foreach ($c in $Candidates) {
    if ($c -eq "mysqldump") {
        $cmd = Get-Command mysqldump -ErrorAction SilentlyContinue
        if ($cmd) { $MysqlDump = $cmd.Source; break }
    } elseif (Test-Path $c) {
        $MysqlDump = $c; break
    }
}
if (-not $MysqlDump) {
    Write-Error "No se encontro mysqldump.exe. Ajusta la lista `$Candidates en el script."
    exit 1
}
Write-Host "Usando mysqldump: $MysqlDump" -ForegroundColor Cyan

# Pasar la contrasena por variable de entorno (evita el warning inseguro)
$env:MYSQL_PWD = $DbPass

$commonArgs = @(
    "--host=$DbHost",
    "--port=$DbPort",
    "--user=$DbUser",
    "--default-character-set=utf8mb4",
    "--no-tablespaces",
    "--single-transaction",
    "--set-gtid-purged=OFF"
)

try {
    # 1) ESTRUCTURA de todo (tablas, vistas, triggers) sin datos
    Write-Host "Exportando estructura completa..." -ForegroundColor Yellow
    $structure = & $MysqlDump @commonArgs --no-data --routines --triggers --add-drop-table $DbName
    if ($LASTEXITCODE -ne 0) { throw "mysqldump (estructura) fallo con codigo $LASTEXITCODE" }

    # 2) DATOS solo de las tablas maestras
    Write-Host "Exportando datos de tablas maestras: $($CatalogTables -join ', ')" -ForegroundColor Yellow
    $dataArgs = $commonArgs + @("--no-create-info", "--skip-triggers", "--complete-insert", "--no-tablespaces")
    $data = & $MysqlDump @dataArgs $DbName @CatalogTables
    if ($LASTEXITCODE -ne 0) { throw "mysqldump (datos) fallo con codigo $LASTEXITCODE" }

    # 3) Quitar clausulas DEFINER de las vistas (portabilidad entre usuarios MySQL)
    $structure = $structure -replace '/\*!50013 DEFINER=[^*]*\*/', ''
    $structure = $structure -replace 'DEFINER=`[^`]*`@`[^`]*`\s*', ''

    # 4) Componer el archivo final
    $header = @"
-- =============================================================================
-- GEA - Dump MAESTRO (estructura completa + datos de catalogos, sin registros)
-- Generado: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
-- Para restaurar:
--   mysql -u root -p < gea_db_maestra.sql
-- =============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
CREATE DATABASE IF NOT EXISTS ``$DbName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ``$DbName``;

"@

    # Semilla de secuencias de Hibernate Envers (sin esto la primera operacion
    # auditada lanza IdentifierGenerationException si las tablas estan vacias)
    $seqSeed = @"

-- ========================= SEMILLAS DE SECUENCIAS ==========================
-- Hibernate Envers: necesita exactamente UNA fila con next_val en cada tabla.
INSERT IGNORE INTO ``audit_revision_info_seq`` (``next_val``) VALUES (1);
INSERT IGNORE INTO ``revinfo_seq`` (``next_val``) VALUES (1);

"@

    $footer = @"

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
-- Fin del dump maestro.
"@

    $content = $header + ($structure -join "`r`n") + "`r`n`r`n" +
               "-- ============================= DATOS MAESTROS =============================`r`n" +
               ($data -join "`r`n") + $seqSeed + $footer

    # Escribir en UTF-8 sin BOM (mysql lo lee limpio)
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($OutFile, $content, $utf8NoBom)

    $sizeKb = [math]::Round((Get-Item $OutFile).Length / 1KB, 1)
    Write-Host "OK -> $OutFile ($sizeKb KB)" -ForegroundColor Green
}
finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}
