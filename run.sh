#!/bin/bash

# Este script automatiza la actualización de la BD y el arranque del servidor en Linux.

echo "=========================================================="
echo "🚀 1. Ejecutando migraciones de Base de Datos..."
echo "=========================================================="
mvn db-migrator:migrate

if [ $? -ne 0 ]; then
  echo "❌ Error al ejecutar las migraciones. Abortando."
  exit 1
fi

echo ""
echo "=========================================================="
echo "🚀 2. Compilando e iniciando el servidor (Spark Java)..."
echo "=========================================================="
# El comando process-classes es necesario para que ActiveJDBC instrumente las clases
mvn clean process-classes exec:java -Dexec.mainClass="com.is1.proyecto.App"
