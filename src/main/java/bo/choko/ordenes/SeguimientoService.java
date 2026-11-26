package bo.choko.ordenes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SeguimientoService {
    public enum Estado {
        SIN_REGISTRO("Sin seguimiento registrado"), RECIBIDA("Recibida"), EN_PROCESO("En proceso"),
        LISTA("Lista para entrega"), ENTREGADA("Entregada");

        public final String etiqueta;
        Estado(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
        public Estado siguiente() {
            return switch (this) {
                case SIN_REGISTRO -> RECIBIDA;
                case RECIBIDA -> EN_PROCESO;
                case EN_PROCESO -> LISTA;
                case LISTA -> ENTREGADA;
                case ENTREGADA -> null;
            };
        }
    }

    public record Cabecera(long id, long clienteId, String nombre, String componente,
                           LocalDate recepcion, Estado estado) {}
    public record Evento(String tipo, String detalle, Integer cantidad, String medio, LocalDate fechaAviso,
                         String usuario, OffsetDateTime registrado) {}
    private final JdbcClient db;
    public SeguimientoService(JdbcClient db) { this.db = db; }

    public Cabecera orden(long id) {
        return db.sql("""
                SELECT o.id, o.cliente_id, c.nombre, o.componente, o.recepcion, o.estado
                FROM ordenes o JOIN clientes c ON c.id=o.cliente_id WHERE o.id=:id
                """).param("id", id).query(Cabecera.class).optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Evento> historial(long id) {
        return db.sql("""
                SELECT tipo, detalle, cantidad, medio, fecha_aviso, usuario, registrado
                FROM seguimiento WHERE orden_id=:id ORDER BY id DESC
                """).param("id", id).query(Evento.class).list();
    }

    // La fila de la orden serializa cambios concurrentes y reintentos del mismo formulario.
    private boolean nuevaSolicitud(long id, UUID solicitud) {
        db.sql("SELECT id FROM ordenes WHERE id=:id FOR UPDATE").param("id", id)
                .query(Long.class).optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return !db.sql("SELECT EXISTS(SELECT 1 FROM seguimiento WHERE orden_id=:id AND solicitud=:s)")
                .param("id", id).param("s", solicitud).query(Boolean.class).single();
    }

    @Transactional
    public void repuesto(long id, UUID solicitud, String nombre, int cantidad, String usuario) {
        if (!nuevaSolicitud(id, solicitud)) return;
        if (cantidad < 1) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        evento(id, solicitud, "REPUESTO", texto(nombre), cantidad, null, null, usuario);
    }

    @Transactional
    public void cambiar(long id, UUID solicitud, Estado nuevo, String usuario) {
        if (!nuevaSolicitud(id, solicitud)) return;
        Estado anterior = orden(id).estado();
        if (anterior.siguiente() != nuevo) throw new IllegalArgumentException("Revise el estado actual de la orden.");
        db.sql("UPDATE ordenes SET estado=:estado WHERE id=:id").param("estado", nuevo.name()).param("id", id).update();
        evento(id, solicitud, "ESTADO", anterior.etiqueta + " → " + nuevo.etiqueta, null, null, null, usuario);
    }

    @Transactional
    public void aviso(long id, UUID solicitud, String medio, LocalDate fecha, String nota, String usuario) {
        if (!nuevaSolicitud(id, solicitud)) return;
        if (!List.of("WhatsApp", "Llamada").contains(medio)) throw new IllegalArgumentException("Medio no válido.");
        if (fecha.isBefore(orden(id).recepcion()) || fecha.isAfter(LocalDate.now(ZoneId.of("America/La_Paz")))) {
            throw new IllegalArgumentException("La fecha del aviso debe estar entre la recepción y hoy.");
        }
        evento(id, solicitud, "AVISO", texto(nota), null, medio, fecha, usuario);
    }

    private String texto(String valor) {
        if (valor.isBlank() || valor.strip().length() > 500) throw new IllegalArgumentException("Ingrese entre 1 y 500 caracteres.");
        return valor.strip();
    }

    private void evento(long id, UUID solicitud, String tipo, String detalle, Integer cantidad,
                        String medio, LocalDate fecha, String usuario) {
        db.sql("""
                INSERT INTO seguimiento (orden_id, solicitud, tipo, detalle, cantidad, medio, fecha_aviso, usuario)
                VALUES (:id,:s,:tipo,:detalle,:cantidad,:medio,:fecha,:usuario)
                """).param("id", id).param("s", solicitud).param("tipo", tipo).param("detalle", detalle)
                .param("cantidad", cantidad).param("medio", medio).param("fecha", fecha).param("usuario", usuario).update();
    }
}
