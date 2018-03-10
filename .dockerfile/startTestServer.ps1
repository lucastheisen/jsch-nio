# Matches src/test/configuration.properties
$tempDir = "$env:TEMP\jsch-nio"
$containerId = ""
try {
    Write-Debug "Creating temp directories"
    New-Item -ItemType Directory -Path $tempDir -Force > $null
    New-Item -ItemType Directory -Path $tempDir\temp -Force > $null

    Write-Debug "Starting sshd..."
    $containerId = & docker run --rm -d -p 2222:22 -v "$tempDir\temp:/home/test" pastdev/jsch-nio-sshd

    Write-Debug "Waiting for sshd to come online"
    $running = $false
    for ($i = 5; $i -gt 0; $i--) {
        Write-Debug "Checking for sshd ($i)"
        if ($(& docker exec $containerId ps -o cmd p 1 --no-headers) | Select-String -Pattern sshd) {
            $running = $true
            break
        }
        Start-Sleep -Seconds 5
    }
    if (!$running) {
        throw [System.TimeoutException] "sshd did not start"
    }

    Write-Debug "Gathering host key"
    Set-Content -Path $tempDir\known_hosts `
        -Value "[localhost]:2222 $(& docker exec $containerId cat /etc/ssh/ssh_host_rsa_key.pub)"

    Write-Debug "Gathering user identity"
    Set-Content -Path $tempDir\id_rsa `
        -Value $(& docker exec $containerId cat /home/test/.ssh/id_rsa)

    Write-Host "`nExit shell to close sshd`n"
    & docker exec -it $containerId bash
}
finally {
    if ($containerId) {
        & docker container stop $containerId
    }
    if (Test-Path -Path $tempDir) {
        Remove-Item -Recurse -Force $tempDir
    }
}
