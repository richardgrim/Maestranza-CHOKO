package bo.choko.ordenes;

import bo.choko.clientes.Cliente;
import bo.choko.clientes.ClienteRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrdenService {
    public record Orden(long id, String componente, String servicios, LocalDate recepcion, LocalDate entregaEstimada) {}

    private final JdbcClient db;
    private final ClienteRepository clientes;

    public OrdenService(JdbcClient db, ClienteRepository clientes) {
        this.db = db;
        this.clientes = clientes;
    }

    public Cliente cliente(long id) {
        return clientes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Orden> listar(long clienteId) {
        return db.sql("""
                SELECT id, componente, servicios, recepcion, entrega_estimada
                FROM ordenes WHERE cliente_id = :cliente ORDER BY id DESC
                """).param("cliente", clienteId).query(Orden.class).list();
    }

    @Transactional
    public void registrar(long clienteId, String componente, String servicios, LocalDate recepcion, LocalDate entrega) {
        cliente(clienteId);
        if (componente.isBlank() || componente.strip().length() > 150) {
            throw new IllegalArgumentException("Indique un componente de hasta 150 caracteres.");
        }
        if (servicios.isBlank() || servicios.strip().length() > 1500) {
            throw new IllegalArgumentException("Describa los servicios solicitados en un máximo de 1500 caracteres.");
        }
        if (recepcion == null || entrega == null || entrega.isBefore(recepcion)) {
            throw new IllegalArgumentException("Complete ambas fechas. La entrega estimada debe ser igual o posterior a la recepción.");
        }
        db.sql("""
                INSERT INTO ordenes (cliente_id, componente, servicios, recepcion, entrega_estimada)
                VALUES (:cliente, :componente, :servicios, :recepcion, :entrega)
                """).param("cliente", clienteId).param("componente", componente.strip())
                .param("servicios", servicios.strip()).param("recepcion", recepcion).param("entrega", entrega).update();
    }
}
