package com.fernando.chat;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            mostrarUso();
            return;
        }

        switch (args[0]) {
            case "servidor": {
                int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
                try (ServidorChat servidor = new ServidorChat(puerto)) {
                    servidor.iniciar();
                    System.out.println("Servidor de chat escuchando en el puerto " + servidor.getPuerto());
                    System.out.println("Presiona Enter para detenerlo.");
                    new Scanner(System.in).nextLine();
                }
                System.out.println("Servidor detenido.");
                break;
            }
            case "cliente": {
                if (args.length < 4) {
                    mostrarUso();
                    return;
                }
                String host = args[1];
                int puerto = Integer.parseInt(args[2]);
                String nick = args[3];

                try (ClienteChat cliente = new ClienteChat(host, puerto, nick)) {
                    cliente.conectar(mensaje -> System.out.println(mensaje.paraMostrar()));
                    System.out.println("Conectado como " + nick + ". Comandos: /privado <nick> <texto>, /salir");

                    Scanner scanner = new Scanner(System.in);
                    while (scanner.hasNextLine()) {
                        String linea = scanner.nextLine();
                        if (linea.equals("/salir")) {
                            cliente.enviarEntradaDeUsuario(linea);
                            break;
                        }
                        try {
                            cliente.enviarEntradaDeUsuario(linea);
                        } catch (IllegalArgumentException e) {
                            System.out.println("-- " + e.getMessage());
                        }
                    }
                }
                System.out.println("Desconectado.");
                break;
            }
            default:
                mostrarUso();
                break;
        }
    }

    private static void mostrarUso() {
        System.out.println("=== Chat con Sockets ===\n"
                + "Uso:\n"
                + "  java -jar chat-sockets-1.0.0.jar servidor [puerto]\n"
                + "  java -jar chat-sockets-1.0.0.jar cliente <host> <puerto> <nick>\n"
                + "\n"
                + "Ejemplo:\n"
                + "  java -jar chat-sockets-1.0.0.jar servidor 5000\n"
                + "  java -jar chat-sockets-1.0.0.jar cliente localhost 5000 daniel");
    }
}
