package com.fernando.chat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Cliente de chat. Un hilo daemon lee del socket y entrega cada mensaje
 * al Consumer que le pasen, para que la UI (o una prueba) decida que hacer.
 */
public class ClienteChat implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader entrada;
    private final PrintWriter salida;
    private final String nick;
    private volatile boolean activo = true;

    public ClienteChat(String host, int puerto, String nick) throws IOException {
        if (nick == null || nick.isBlank()) {
            throw new IllegalArgumentException("El nick no puede estar vacio");
        }
        this.nick = nick.trim();
        this.socket = new Socket(host, puerto);
        this.entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public String getNick() {
        return nick;
    }

    /** Anuncia la entrada al chat y arranca el hilo lector. */
    public void conectar(Consumer<Mensaje> alRecibir) {
        enviarCrudo(new Mensaje(Mensaje.Tipo.ENTRADA, nick, ""));
        Thread lector = new Thread(() -> {
            try {
                String linea;
                while (activo && (linea = entrada.readLine()) != null) {
                    alRecibir.accept(Protocolo.deserializar(linea));
                }
            } catch (IOException e) {
                if (activo) {
                    alRecibir.accept(Mensaje.sistema("Conexion perdida: " + e.getMessage()));
                }
            }
        }, "chat-cliente-lector");
        lector.setDaemon(true);
        lector.start();
    }

    /** Envia lo que escribio el usuario, interpretando los comandos. */
    public void enviarEntradaDeUsuario(String entradaUsuario) {
        enviarCrudo(Protocolo.interpretarEntrada(nick, entradaUsuario));
    }

    public void enviarCrudo(Mensaje mensaje) {
        salida.println(Protocolo.serializar(mensaje));
    }

    @Override
    public void close() {
        activo = false;
        try {
            socket.close();
        } catch (IOException ignored) {
            // ya estaba cerrado
        }
    }
}
