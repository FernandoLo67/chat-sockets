# Chat con Sockets

Chat multiusuario cliente-servidor sobre TCP, escrito solo con la biblioteca estandar de Java. El servidor atiende a varios clientes a la vez, difunde los mensajes al grupo y soporta mensajes privados.

## Comandos disponibles

| Comando | Que hace |
|---|---|
| *texto normal* | Lo ve todo el mundo |
| `/privado <nick> <texto>` | Solo lo ve ese usuario |
| `/salir` | Cierra la sesion |

## Como ejecutar

En una terminal, el servidor:

```bash
mvn package
java -jar target/chat-sockets-1.0.0.jar servidor 5000
```

En otra terminal (una por cada persona), un cliente:

```bash
java -jar target/chat-sockets-1.0.0.jar cliente localhost 5000 daniel
```

## Que muestra este proyecto

- Sockets TCP (`ServerSocket` / `Socket`) sin frameworks.
- Concurrencia: un pool de hilos daemon atiende a cada cliente y `CopyOnWriteArrayList` mantiene la lista de conexiones sin condiciones de carrera.
- Un protocolo de texto propio (`TIPO|remitente|contenido`) con escapado del separador, para que un mensaje que contenga `|` no rompa el parseo.
- Cierre ordenado de recursos con `AutoCloseable` y try-with-resources.
- **Pruebas de integracion reales**: `ServidorChatTest` levanta el servidor en un puerto libre, conecta clientes de verdad sobre loopback y verifica la difusion y los privados con colas bloqueantes y timeouts.

## Como correr las pruebas

```bash
mvn test
```
