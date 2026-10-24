package com.fernando.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitadorDeMensajesTest {

    @Test
    void permiteHastaElLimite() {
        long[] ahora = {0L};
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(3, 10_000, () -> ahora[0]);

        assertTrue(limitador.permitir("ana"));
        assertTrue(limitador.permitir("ana"));
        assertTrue(limitador.permitir("ana"));
        assertFalse(limitador.permitir("ana"), "El cuarto mensaje excede el limite de 3");
    }

    @Test
    void cadaUsuarioTieneSuPropioLimite() {
        long[] ahora = {0L};
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(2, 10_000, () -> ahora[0]);

        assertTrue(limitador.permitir("ana"));
        assertTrue(limitador.permitir("ana"));
        assertFalse(limitador.permitir("ana"));

        assertTrue(limitador.permitir("pedro"), "Pedro tiene su propio cupo");
        assertTrue(limitador.permitir("pedro"));
    }

    @Test
    void laVentanaSeDeslizaYLiberaCupo() {
        long[] ahora = {0L};
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(2, 5_000, () -> ahora[0]);

        assertTrue(limitador.permitir("ana"));
        ahora[0] = 2_000;
        assertTrue(limitador.permitir("ana"));
        assertFalse(limitador.permitir("ana"), "Aun dentro de la ventana");

        ahora[0] = 5_001;
        assertTrue(limitador.permitir("ana"), "El primer mensaje ya expiro, hay cupo");
    }

    @Test
    void mensajesRestantesReflejaElEstado() {
        long[] ahora = {0L};
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(3, 10_000, () -> ahora[0]);

        assertEquals(3, limitador.mensajesRestantes("ana"));

        limitador.permitir("ana");
        assertEquals(2, limitador.mensajesRestantes("ana"));

        limitador.permitir("ana");
        limitador.permitir("ana");
        assertEquals(0, limitador.mensajesRestantes("ana"));
    }

    @Test
    void mensajesRestantesSeRecuperaConLaVentana() {
        long[] ahora = {0L};
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(2, 5_000, () -> ahora[0]);

        limitador.permitir("ana");
        limitador.permitir("ana");
        assertEquals(0, limitador.mensajesRestantes("ana"));

        ahora[0] = 5_001;
        assertEquals(2, limitador.mensajesRestantes("ana"), "Toda la ventana expiro");
    }

    @Test
    void reiniciarLibraAlUsuario() {
        long[] ahora = {0L};
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(1, 10_000, () -> ahora[0]);

        limitador.permitir("ana");
        assertFalse(limitador.permitir("ana"));

        limitador.reiniciar("ana");
        assertTrue(limitador.permitir("ana"), "Despues de reiniciar, vuelve a tener cupo");
    }

    @Test
    void reiniciarTodosLimpiaTodosLosUsuarios() {
        long[] ahora = {0L};
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(1, 10_000, () -> ahora[0]);

        limitador.permitir("ana");
        limitador.permitir("pedro");

        limitador.reiniciarTodos();

        assertTrue(limitador.permitir("ana"));
        assertTrue(limitador.permitir("pedro"));
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new LimitadorDeMensajes(0, 10_000, System::currentTimeMillis));
        assertThrows(IllegalArgumentException.class,
                () -> new LimitadorDeMensajes(5, 0, System::currentTimeMillis));
        assertThrows(IllegalArgumentException.class,
                () -> new LimitadorDeMensajes(5, -1, System::currentTimeMillis));
    }

    @Test
    void usuarioDesconocidoTieneMaximoRestante() {
        LimitadorDeMensajes limitador = new LimitadorDeMensajes(5, 10_000, System::currentTimeMillis);
        assertEquals(5, limitador.mensajesRestantes("desconocido"));
    }
}
