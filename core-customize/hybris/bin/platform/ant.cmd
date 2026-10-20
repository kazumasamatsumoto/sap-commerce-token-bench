@echo off
rem ant for Windows. Use it in hybris\bin\platform like: ant clean all . It runs inside Docker.
rem In PowerShell, quote -D options: ant importImpex '"-Dresource=..."'
set HERE=%~dp0
docker compose -f "%HERE%..\..\..\..\docker-compose.yml" --profile tools run --rm -T builder bash resources/ant/ant-impl.sh %*
