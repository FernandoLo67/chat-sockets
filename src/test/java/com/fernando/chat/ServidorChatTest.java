package com.fernando.chat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion reales: levantan el servidor en un puerto libre
 * y conectan clientes de verdad sobre loopback.
 */
class ServidorChatTest {

    private static final int TIMEOUT_SEGUNDOS = 5;

    /** Espera un mensaje del tipo pedido, descartando los de sistema/bienvenida. */
    private Mensaje esperar(BlockingQueue<Mensaje> cola, Mensaje.Tipo tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + TIMEOUT_SEGUNDOS * 1000L;
        while (System.currentTimeMillis() < limite) {
            Mensaje mensaje = cola.poll(500, TimeUnit.MILLISECONDS);
            if (mensaje != null && mensaje.tipo() == tipo) {
                return mensaje;
            }
        }
        fail("No llego ningun mensaje de tipo " + tipo + " en " + TIMEOUT_SEGUNDOS + "s");
        return null;
    }

    @Test
    void elServidorAbreUnPuertoValido() throws IOException {
        try (ServidorChat servidor = new ServidorChat(0)) {
            servidor.iniciar();
            assertTrue(servidor.getPuerto() > 0, "El puerto asignado deberia ser mayor a 0");
        }
    }

    @Test
    void unMensajeDeUnClienteLlegaAlOtro() throws Exception {
        try (ServidorChat servidor = new ServidorChat(0)) {
            servidor.iniciar();
            BlockingQueue<Mensaje> recibidosPorAna = new LinkedBlockingQueue<>();

            try (ClienteChat daniel = new ClienteChat("127.0.0.1", servidor.getPuerto(), "daniel");
                 ClienteChat ana = new ClienteChat("127.0.0.1", servidor.getPuerto(), "ana")) {

                daniel.conectar(m -> { });
                ana.conectar(recibidosPorAna::add);
                Thread.sleep(300); // dar tiempo a que ambos se registren

                daniel.enviarEntradaDeUsuario("hola a todos");

                Mensaje recibido = esperar(recibidosPorAna, Mensaje.Tipo.TEXTO);
                assertEquals("daniel", recibido.remitente());
                assertEquals("hola a todos", recibido.contenido());
            }
        }
    }

    @Test
    void elMensajePrivadoSoloLlegaAlDestinatario() throws Exception {
        try (ServidorChat servidor = new ServidorChat(0)) {
            servidor.iniciar();
            BlockingQueue<Mensaje> recibidosPorAna = new LinkedBlockingQueue<>();
            BlockingQueue<Mensaje> recibidosPorBeto = new LinkedBlockingQueue<>();

            try (ClienteChat daniel = new ClienteChat("127.0.0.1", servidor.getPuerto(), "daniel");
                 ClienteChat ana = new ClienteChat("127.0.0.1", servidor.getPuerto(), "ana");
                 ClienteChat beto = new ClienteChat("127.0.0.1", servidor.getPuerto(), "beto")) {

                daniel.conectar(m -> { });
                ana.conectar(recibidosPorAna::add);
                beto.conectar(recibidosPorBeto::add);
                Thread.sleep(300);
                recibidosPorBeto.clear();

                daniel.enviarEntradaDeUsuario("/privado ana solo para vos");

                Mensaje recibido = esperar(recibidosPorAna, Mensaje.Tipo.PRIVADO);
                assertEquals("daniel", recibido.remitente());
                assertEquals("solo para vos", recibido.contenido());

                // Beto no deberia recibir nada de tipo PRIVADO ni TEXTO
                Mensaje colado = recibidosPorBeto.poll(1, TimeUnit.SECONDS);
                if (colado != null) {
                    assertNotEquals(Mensaje.Tipo.PRIVADO, colado.tipo(), "Beto no deberia ver el privado");
                    assertNotEquals(Mensaje.Tipo.TEXTO, colado.tipo(), "Beto no deberia ver el privado");
                }
            }
        }
    }

    @Test
    void elServidorAvisaCuandoAlguienSeUne() throws Exception {
        try (ServidorChat servidor = new ServidorChat(0)) {
            servidor.iniciar();
            BlockingQueue<Mensaje> recibidosPorDaniel = new LinkedBlockingQueue<>();

            try (ClienteChat daniel = new ClienteChat("127.0.0.1", servidor.getPuerto(), "daniel")) {
                daniel.conectar(recibidosPorDaniel::add);
                Thread.sleep(300);

                try (ClienteChat ana = new ClienteChat("127.0.0.1", servidor.getPuerto(), "ana")) {
                    ana.conectar(m -> { });
                    Mensaje aviso = esperar(recibidosPorDaniel, Mensaje.Tipo.ENTRADA);
                    assertEquals("ana", aviso.remitente());
                }
            }
        }
    }

    @Test
    void elServidorCuentaLosClientesConectados() throws Exception {
        try (ServidorChat servidor = new ServidorChat(0)) {
            servidor.iniciar();
            assertEquals(0, servidor.clientesConectados());

            try (ClienteChat daniel = new ClienteChat("127.0.0.1", servidor.getPuerto(), "daniel");
                 ClienteChat ana = new ClienteChat("127.0.0.1", servidor.getPuerto(), "ana")) {
                daniel.conectar(m -> { });
                ana.conectar(m -> { });
                Thread.sleep(500);

                assertEquals(2, servidor.clientesConectados());
                assertTrue(servidor.nicksConectados().contains("daniel"));
                assertTrue(servidor.nicksConectados().contains("ana"));
            }
        }
    }

    @Test
    void nickVacioEsRechazadoEnElCliente() throws IOException {
        try (ServidorChat servidor = new ServidorChat(0)) {
            servidor.iniciar();
            assertThrows(IllegalArgumentException.class,
                    () -> new ClienteChat("127.0.0.1", servidor.getPuerto(), "  "));
        }
    }
}
