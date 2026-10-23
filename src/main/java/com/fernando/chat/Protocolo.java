package com.fernando.chat;

/**
 * Traduce entre objetos Mensaje y las lineas de texto que viajan por el socket,
 * y ademas interpreta los comandos que escribe el usuario (/salir, /privado, ...).
 *
 * Formato de linea: TIPO|remitente|contenido
 * Los separadores dentro del contenido se escapan para no romper el parseo.
 */
public final class Protocolo {

    public static final String SEPARADOR = "|";
    private static final String SEPARADOR_ESCAPADO = "\\u0001";

    private Protocolo() {
    }

    public static String serializar(Mensaje mensaje) {
        return mensaje.tipo().name()
                + SEPARADOR + escapar(mensaje.remitente())
                + SEPARADOR + escapar(mensaje.contenido());
    }

    public static Mensaje deserializar(String linea) {
        if (linea == null || linea.isBlank()) {
            throw new IllegalArgumentException("Linea vacia");
        }
        String[] partes = linea.split("\\" + SEPARADOR, 3);
        if (partes.length != 3) {
            throw new IllegalArgumentException("Linea mal formada: " + linea);
        }
        Mensaje.Tipo tipo;
        try {
            tipo = Mensaje.Tipo.valueOf(partes[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de mensaje desconocido: " + partes[0]);
        }
        return new Mensaje(tipo, desescapar(partes[1]), desescapar(partes[2]));
    }

    /**
     * Interpreta lo que el usuario escribio en la consola.
     * "/salir" -> SALIDA, "/privado nick texto" -> PRIVADO, cualquier otra cosa -> TEXTO.
     */
    public static Mensaje interpretarEntrada(String remitente, String entrada) {
        String texto = entrada == null ? "" : entrada.trim();
        if (texto.equals("/salir")) {
            return new Mensaje(Mensaje.Tipo.SALIDA, remitente, "");
        }
        if (texto.startsWith("/privado ")) {
            String resto = texto.substring("/privado ".length()).trim();
            int espacio = resto.indexOf(' ');
            if (espacio <= 0) {
                throw new IllegalArgumentException("Uso: /privado <nick> <mensaje>");
            }
            String destinatario = resto.substring(0, espacio);
            String cuerpo = resto.substring(espacio + 1).trim();
            if (cuerpo.isEmpty()) {
                throw new IllegalArgumentException("Uso: /privado <nick> <mensaje>");
            }
            return new Mensaje(Mensaje.Tipo.PRIVADO, remitente, destinatario + " " + cuerpo);
        }
        if (texto.startsWith("/")) {
            throw new IllegalArgumentException("Comando desconocido: " + texto);
        }
        return Mensaje.texto(remitente, texto);
    }

    /** Del contenido de un PRIVADO extrae el nick destino. */
    public static String destinatarioDe(Mensaje privado) {
        exigirPrivado(privado);
        int espacio = privado.contenido().indexOf(' ');
        return espacio < 0 ? privado.contenido() : privado.contenido().substring(0, espacio);
    }

    /** Del contenido de un PRIVADO extrae el texto sin el nick destino. */
    public static String cuerpoDe(Mensaje privado) {
        exigirPrivado(privado);
        int espacio = privado.contenido().indexOf(' ');
        return espacio < 0 ? "" : privado.contenido().substring(espacio + 1);
    }

    private static void exigirPrivado(Mensaje mensaje) {
        if (mensaje.tipo() != Mensaje.Tipo.PRIVADO) {
            throw new IllegalArgumentException("El mensaje no es de tipo PRIVADO");
        }
    }

    private static String escapar(String texto) {
        return texto.replace(SEPARADOR, SEPARADOR_ESCAPADO);
    }

    private static String desescapar(String texto) {
        return texto.replace(SEPARADOR_ESCAPADO, SEPARADOR);
    }
}
