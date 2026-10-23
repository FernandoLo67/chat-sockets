package com.fernando.chat;

import java.util.Objects;

/**
 * Mensaje que viaja por el socket. Se serializa como TIPO|remitente|contenido.
 */
public final class Mensaje {

    private final Tipo tipo;
    private final String remitente;
    private final String contenido;

    public enum Tipo { ENTRADA, SALIDA, TEXTO, PRIVADO, SISTEMA }

    public Mensaje(Tipo tipo, String remitente, String contenido) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo no puede ser nulo");
        }
        if (remitente == null) {
            remitente = "";
        }
        if (contenido == null) {
            contenido = "";
        }
        this.tipo = tipo;
        this.remitente = remitente;
        this.contenido = contenido;
    }

    public Tipo tipo() {
        return tipo;
    }

    public String remitente() {
        return remitente;
    }

    public String contenido() {
        return contenido;
    }

    public static Mensaje texto(String remitente, String contenido) {
        return new Mensaje(Tipo.TEXTO, remitente, contenido);
    }

    public static Mensaje sistema(String contenido) {
        return new Mensaje(Tipo.SISTEMA, "servidor", contenido);
    }

    /** Como se muestra en pantalla. */
    public String paraMostrar() {
        switch (tipo) {
            case ENTRADA:
                return "* " + remitente + " se unio al chat";
            case SALIDA:
                return "* " + remitente + " salio del chat";
            case TEXTO:
                return "[" + remitente + "] " + contenido;
            case PRIVADO:
                return "(privado de " + remitente + ") " + contenido;
            case SISTEMA:
                return "-- " + contenido;
            default:
                throw new IllegalStateException("Tipo desconocido: " + tipo);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mensaje)) {
            return false;
        }
        Mensaje other = (Mensaje) o;
        return tipo == other.tipo
                && Objects.equals(remitente, other.remitente)
                && Objects.equals(contenido, other.contenido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, remitente, contenido);
    }

    @Override
    public String toString() {
        return "Mensaje[tipo=" + tipo + ", remitente=" + remitente + ", contenido=" + contenido + "]";
    }
}
