package com.fernando.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Servidor de chat multiusuario. Cada cliente se atiende en su propio hilo
 * (pool de hilos daemon) y la lista de conexiones es concurrente porque la
 * leen y modifican varios hilos a la vez.
 */
public class ServidorChat implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private final CopyOnWriteArrayList<Conexion> conexiones = new CopyOnWriteArrayList<>();
    private volatile boolean activo;

    /** Puerto 0 = el sistema operativo asigna uno libre (util en pruebas). */
    public ServidorChat(int puerto) throws IOException {
        this.serverSocket = new ServerSocket(puerto);
        this.pool = Executors.newCachedThreadPool(tarea -> {
            Thread hilo = new Thread(tarea, "chat-servidor");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public int getPuerto() {
        return serverSocket.getLocalPort();
    }

    public void iniciar() {
        activo = true;
        pool.submit(this::aceptarConexiones);
    }

    private void aceptarConexiones() {
        while (activo && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Conexion conexion = new Conexion(socket);
                conexiones.add(conexion);
                pool.submit(conexion);
            } catch (IOException e) {
                if (activo) {
                    System.err.println("Error aceptando conexion: " + e.getMessage());
                }
                return;
            }
        }
    }

    public int clientesConectados() {
        return (int) conexiones.stream().filter(c -> c.nick != null).count();
    }

    public List<String> nicksConectados() {
        return conexiones.stream().map(c -> c.nick).filter(java.util.Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    void difundir(Mensaje mensaje, Conexion excepto) {
        for (Conexion conexion : conexiones) {
            if (conexion != excepto && conexion.nick != null) {
                conexion.enviar(mensaje);
            }
        }
    }

    private Optional<Conexion> buscarPorNick(String nick) {
        return conexiones.stream().filter(c -> nick.equals(c.nick)).findFirst();
    }

    @Override
    public void close() {
        activo = false;
        for (Conexion conexion : conexiones) {
            conexion.cerrar();
        }
        conexiones.clear();
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // el servidor ya estaba cerrado
        }
        pool.shutdownNow();
    }

    /** Una conexion de cliente vista desde el servidor. */
    private class Conexion implements Runnable {

        private final Socket socket;
        private final BufferedReader entrada;
        private final PrintWriter salida;
        private volatile String nick;

        Conexion(Socket socket) throws IOException {
            this.socket = socket;
            this.entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try {
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    procesar(Protocolo.deserializar(linea));
                }
            } catch (SocketException e) {
                // el cliente cerro la conexion de golpe: es normal
            } catch (IOException | IllegalArgumentException e) {
                enviar(Mensaje.sistema("Error: " + e.getMessage()));
            } finally {
                desconectar();
            }
        }

        private void procesar(Mensaje mensaje) {
            switch (mensaje.tipo()) {
                case ENTRADA: {
                    nick = mensaje.remitente();
                    enviar(Mensaje.sistema("Bienvenido, " + nick + ". Conectados: " + clientesConectados()));
                    difundir(new Mensaje(Mensaje.Tipo.ENTRADA, nick, ""), this);
                    break;
                }
                case TEXTO:
                    difundir(Mensaje.texto(nick, mensaje.contenido()), this);
                    break;
                case PRIVADO: {
                    String destino = Protocolo.destinatarioDe(mensaje);
                    String cuerpo = Protocolo.cuerpoDe(mensaje);
                    buscarPorNick(destino).ifPresentOrElse(
                            c -> c.enviar(new Mensaje(Mensaje.Tipo.PRIVADO, nick, cuerpo)),
                            () -> enviar(Mensaje.sistema("No hay nadie conectado con el nick " + destino)));
                    break;
                }
                case SALIDA:
                    cerrar();
                    break;
                case SISTEMA:
                    /* el cliente no envia mensajes de sistema */
                    break;
                default:
                    break;
            }
        }

        void enviar(Mensaje mensaje) {
            salida.println(Protocolo.serializar(mensaje));
        }

        private void desconectar() {
            if (conexiones.remove(this) && nick != null) {
                difundir(new Mensaje(Mensaje.Tipo.SALIDA, nick, ""), this);
            }
            cerrar();
        }

        void cerrar() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // ya estaba cerrado
            }
        }
    }
}
