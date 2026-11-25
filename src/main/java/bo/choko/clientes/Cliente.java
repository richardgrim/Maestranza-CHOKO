package bo.choko.clientes;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 40)
    private String telefono;

    protected Cliente() {}

    public Cliente(String nombre, String telefono) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 150) {
            throw new IllegalArgumentException("El nombre debe tener entre 1 y 150 caracteres.");
        }
        this.nombre = nombre.strip();
        this.telefono = telefono == null || telefono.isBlank() ? null : telefono.strip();
        if (this.telefono != null && this.telefono.length() > 40) {
            throw new IllegalArgumentException("El teléfono admite hasta 40 caracteres.");
        }
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
}
