package bo.choko.clientes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clientes;

    public ClienteService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional
    public void registrar(String nombre, String telefono) {
        clientes.save(new Cliente(nombre, telefono));
    }

    @Transactional(readOnly = true)
    public Page<Cliente> buscar(String nombre, int pagina) {
        return clientes.findByNombreContainingIgnoreCaseOrderByNombreAscIdAsc(
                nombre.strip(), PageRequest.of(Math.max(0, pagina), 20));
    }
}
