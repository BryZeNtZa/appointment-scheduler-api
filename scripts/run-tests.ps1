# Runs the full test suite (unit + Testcontainers integration) inside a Maven
# container, so no JDK/Maven is required on the host - only Docker.
#
# The Docker socket is mounted so Testcontainers can start a sibling Postgres
# container. DOCKER_API_VERSION/api.version are pinned because the bundled
# docker-java client cannot negotiate the API version of Docker Engine >= 29.

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

Write-Host "Running tests in a Maven container (no host JDK needed)."
Write-Host "First run downloads dependencies and images - this can take a few minutes. Progress streams live below." -ForegroundColor Yellow

# Pre-pull the Postgres image on the host (with a visible progress bar) when it
# is not cached. Testcontainers shares the host daemon via the mounted socket,
# so it then reuses this image instead of pulling it silently in-container.
docker image inspect postgres:16-alpine *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Pulling postgres:16-alpine ..." -ForegroundColor Yellow
    docker pull postgres:16-alpine
}

# Interactive console: allocate a TTY (-t) so output streams live, and omit
# Maven batch mode (-B) so Maven shows its live download/transfer progress meter.
# Redirected/CI: keep -B for clean logs and drop -t (which would hang).
$ttyFlag = @()
$mvnBatch = @("-B")
if (-not [Console]::IsOutputRedirected) { $ttyFlag = @("-t"); $mvnBatch = @() }

docker run --rm @ttyFlag `
  -v "${repoRoot}:/app" `
  -v afb_m2:/root/.m2 `
  -v "/var/run/docker.sock:/var/run/docker.sock" `
  -e DOCKER_HOST=unix:///var/run/docker.sock `
  -e DOCKER_API_VERSION=1.44 `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  -e TESTCONTAINERS_RYUK_DISABLED=true `
  --add-host=host.docker.internal:host-gateway `
  -w /app `
  maven:3.9-eclipse-temurin-21 mvn @mvnBatch test -DargLine="-Dapi.version=1.44"
