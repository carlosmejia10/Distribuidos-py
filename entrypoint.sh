#!/bin/sh

# Ejecutar Maven con los argumentos proporcionados
exec mvn exec:java -Dexec.mainClass=com.example.HealthCheck -Dexec.args="$@"