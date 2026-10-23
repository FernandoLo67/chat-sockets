package com.fernando.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocoloTest {

    @Test
    void serializarYDeserializarConservaElMensaje() {
        Mensaje original = Mensaje.texto("daniel", "hola mundo");
        Mensaje recuperado = Protocolo.deserializar(Protocolo.serializar(original));

        assertEquals(original.tipo(), recuperado.tipo());
        assertEquals("daniel", recuperado.remitente());
        assertEquals("hola mundo", recuperado.contenido());
    }

    @Test
    void elContenidoConSeparadorNoRompeElParseo() {
        Mensaje original = Mensaje.texto("ana", "a|b|c");
        Mensaje recuperado = Protocolo.deserializar(Protocolo.serializar(original));
        assertEquals("a|b|c", recuperado.contenido());
    }

    @Test
    void lineaMalFormadaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> Protocolo.deserializar("TEXTO|solo-dos-campos"));
    }

    @Test
    void tipoDesconocidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> Protocolo.deserializar("INVENTADO|a|b"));
    }

    @Test
    void lineaVaciaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> Protocolo.deserializar("   "));
    }

    @Test
    void textoNormalSeInterpretaComoMensajeDeTexto() {
        Mensaje mensaje = Protocolo.interpretarEntrada("daniel", "buenas");
        assertEquals(Mensaje.Tipo.TEXTO, mensaje.tipo());
        assertEquals("buenas", mensaje.contenido());
    }

    @Test
    void comandoSalirSeInterpretaComoSalida() {
        assertEquals(Mensaje.Tipo.SALIDA, Protocolo.interpretarEntrada("daniel", "/salir").tipo());
    }

    @Test
    void comandoPrivadoSeparaDestinatarioYCuerpo() {
        Mensaje mensaje = Protocolo.interpretarEntrada("daniel", "/privado ana nos vemos luego");

        assertEquals(Mensaje.Tipo.PRIVADO, mensaje.tipo());
        assertEquals("ana", Protocolo.destinatarioDe(mensaje));
        assertEquals("nos vemos luego", Protocolo.cuerpoDe(mensaje));
    }

    @Test
    void comandoPrivadoSinCuerpoEsRechazado() {
        assertThrows(IllegalArgumentException.class, () -> Protocolo.interpretarEntrada("daniel", "/privado ana"));
    }

    @Test
    void comandoDesconocidoEsRechazado() {
        assertThrows(IllegalArgumentException.class, () -> Protocolo.interpretarEntrada("daniel", "/bailar"));
    }

    @Test
    void cuerpoDeExigeMensajePrivado() {
        assertThrows(IllegalArgumentException.class, () -> Protocolo.cuerpoDe(Mensaje.texto("a", "b")));
    }

    @Test
    void formatoDeVisualizacionSegunElTipo() {
        assertEquals("[ana] hola", Mensaje.texto("ana", "hola").paraMostrar());
        assertEquals("* ana se unio al chat", new Mensaje(Mensaje.Tipo.ENTRADA, "ana", "").paraMostrar());
        assertEquals("* ana salio del chat", new Mensaje(Mensaje.Tipo.SALIDA, "ana", "").paraMostrar());
    }
}
