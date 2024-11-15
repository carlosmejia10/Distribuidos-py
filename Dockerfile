# Usa una imagen base de Maven con OpenJDK 17 para construir y ejecutar el proyecto
FROM maven:3.8.4-openjdk-17

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo pom.xml y el código fuente al contenedor
COPY pom.xml .
COPY src /app/src

# Descarga las dependencias de Maven y compila el proyecto
RUN mvn clean compile

# Expone el puerto si la aplicación lo requiere (ajusta según sea necesario)
EXPOSE 5555

# Comando para ejecutar la aplicación Java usando el plugin exec-maven-plugin
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.example.ServidorCentral"]