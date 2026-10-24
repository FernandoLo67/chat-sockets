package com.fernando.chat;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongSupplier;

/**
 * Rate limiter por usuario con ventana deslizante.
 *
 * <p>Cada usuario puede enviar como maximo {@code maxMensajes} dentro de una
 * ventana de {@code ventanaMs} milisegundos. Si la excede, los mensajes
 * siguientes se rechazan hasta que la ventana se desplace y libere cupo.
 *
 * <p>Usa una cola de marcas de tiempo por usuario y un reloj inyectable
 * para que las pruebas no dependan de Thread.sleep.
 */
public class LimitadorDeMensajes {

    private final int maxMensajes;
    private final long ventanaMs;
    private final LongSupplier reloj;
    private final Map<String, Queue<Long>> registroPorUsuario = new ConcurrentHashMap<>();

    public LimitadorDeMensajes(int maxMensajes, long ventanaMs, LongSupplier reloj) {
        if (maxMensajes < 1) {
            throw new IllegalArgumentException("El limite debe ser al menos 1 mensaje");
        }
        if (ventanaMs <= 0) {
            throw new IllegalArgumentException("La ventana debe ser positiva");
        }
        this.maxMensajes = maxMensajes;
        this.ventanaMs = ventanaMs;
        this.reloj = reloj;
    }

    /**
     * Intenta registrar un mensaje del usuario.
     *
     * @return true si el mensaje se permite, false si se excedio el limite
     */
    public boolean permitir(String usuario) {
        long ahora = reloj.getAsLong();
        Queue<Long> marcas = registroPorUsuario.computeIfAbsent(usuario, u -> new ConcurrentLinkedQueue<>());

        purgarExpiradas(marcas, ahora);

        if (marcas.size() >= maxMensajes) {
            return false;
        }

        marcas.add(ahora);
        return true;
    }

    public int mensajesRestantes(String usuario) {
        Queue<Long> marcas = registroPorUsuario.get(usuario);
        if (marcas == null) {
            return maxMensajes;
        }
        purgarExpiradas(marcas, reloj.getAsLong());
        return Math.max(0, maxMensajes - marcas.size());
    }

    public void reiniciar(String usuario) {
        registroPorUsuario.remove(usuario);
    }

    public void reiniciarTodos() {
        registroPorUsuario.clear();
    }

    private void purgarExpiradas(Queue<Long> marcas, long ahora) {
        long limite = ahora - ventanaMs;
        while (!marcas.isEmpty() && marcas.peek() <= limite) {
            marcas.poll();
        }
    }
}
