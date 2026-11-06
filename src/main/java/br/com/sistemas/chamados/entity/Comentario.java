package br.com.sistemas.chamados.entity;

import br.com.sistemas.chamados.enums.TipoAutor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_autor", nullable = false)
    private TipoAutor tipoAutor;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "tecnico_id")
    private Tecnico tecnico;

    @ManyToOne
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    public Comentario() {
    }

    public Comentario(String texto, Chamado chamado, Cliente autor) {
        this.texto = texto;
        this.dataCriacao = LocalDateTime.now();
        this.tipoAutor = TipoAutor.CLIENTE;
        this.cliente = autor;
        this.tecnico = null;
        this.chamado = chamado;
    }

    public Comentario(String texto, Chamado chamado, Tecnico autor) {
        this.texto = texto;
        this.dataCriacao = LocalDateTime.now();
        this.tipoAutor = TipoAutor.TECNICO;
        this.cliente = null;
        this.tecnico = autor;
        this.chamado = chamado;
    }

    public Long getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public TipoAutor getTipoAutor() {
        return tipoAutor;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Tecnico getTecnico() {
        return tecnico;
    }

    public Chamado getChamado() {
        return chamado;
    }
}
