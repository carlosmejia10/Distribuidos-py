# Uber Simulation

## Requisitos

- Java Development Kit (JDK) 8 o superior
- Apache Maven 3.6.0 o superior

## Configuración de IPs

Asegúrate de cambiar las direcciones IP en los archivos `Taxi.java` y `Usuario.java` para que apunten a la IP del servidor central.

### Taxi.java
```java
this.socket.connect("tcp://<IP_DEL_SERVIDOR_CENTRAL>:5555");
