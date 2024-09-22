# Uber Simulation

## Requisitos

- Java Development Kit (JDK) 8 o superior
- Apache Maven 3.6.0 o superior

## Configuración de IPs

Asegúrate de cambiar las direcciones IP en los archivos `Taxi.java` y `Usuario.java` para que apunten a la IP del servidor central.

### Taxi.java
```java
this.socket.connect("tcp://<IP_DEL_SERVIDOR_CENTRAL>:5555");
```
### Usuario.java
```java
this.socket.connect("tcp://192.168.10.24:5555");
```
## Ejecucion

```java
mvn compile
```
### Ejecutar los Taxis: Abre una nueva terminal para cada taxi y ejecuta (ejemplo):
```java
mvn exec:java -Dexec.mainClass=com.example.Taxi -Dexec.args="1 1000 1000 0 0 4 3"
mvn exec:java -Dexec.mainClass=com.example.Taxi -Dexec.args="2 1000 1000 1 1 4 3"
```

### Ejecutar los Usuarios: Abre una nueva terminal para cada usuario y ejecuta (ejemplo):
```java
mvn exec:java -Dexec.mainClass=com.example.Usuario -Dexec.args="1 0 0 5"
mvn exec:java -Dexec.mainClass=com.example.Usuario -Dexec.args="2 1 1 10"
```
# En la segunda maquina compilar el proyecto y ejecutar:
```java
mvn exec:java -Dexec.mainClass="com.example.ServidorCentral"
```



