
$connectionString = "Server=localhost;Port=3306;Uid=root;Pwd=1234;Database=mysql;"
try {
    $conn = New-Object MySql.Data.MySqlClient.MySqlConnection($connectionString)
    $conn.Open()
    Write-Host "Connected successfully with password 1234"
    $conn.Close()
} catch {
    Write-Host "Failed to connect with password 1234: $($_.Exception.Message)"
}

$connectionStringNoPwd = "Server=localhost;Port=3306;Uid=root;Database=mysql;"
try {
    $conn = New-Object MySql.Data.MySqlClient.MySqlConnection($connectionStringNoPwd)
    $conn.Open()
    Write-Host "Connected successfully with NO password"
    $conn.Close()
} catch {
    Write-Host "Failed to connect with NO password: $($_.Exception.Message)"
}
