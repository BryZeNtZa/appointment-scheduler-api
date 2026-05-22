# Runs the full test suite (unit + Testcontainers integration) inside a Maven
# container, so no JDK/Maven is required on the host - only Docker.
#
# The Docker socket is mounted so Testcontainers can start a sibling Postgres
# container. DOCKER_API_VERSION/api.version are pinned because the bundled
# docker-java client cannot negotiate the API version of Docker Engine >= 29.

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

docker run --rm `
  -v "${repoRoot}:/app" `
  -v afb_m2:/root/.m2 `
  -v "/var/run/docker.sock:/var/run/docker.sock" `
  -e DOCKER_HOST=unix:///var/run/docker.sock `
  -e DOCKER_API_VERSION=1.44 `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  -e TESTCONTAINERS_RYUK_DISABLED=true `
  --add-host=host.docker.internal:host-gateway `
  -w /app `
  maven:3.9-eclipse-temurin-21 mvn -B test -DargLine="-Dapi.version=1.44"
