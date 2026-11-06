package br.com.sistemas.chamados.entity;

import br.com.sistemas.chamados.enums.StatusChamado;
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
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusChamado statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusChamado statusNovo;

    @Column(name = "data_alteracao", nullable = false)
    private LocalDateTime dataAlteracao;

    @ManyToOne
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    public HistoricoStatus() {
    }

    public HistoricoStatus(StatusChamado statusAnterior, StatusChamado statusNovo, Chamado chamado) {
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.dataAlteracao = LocalDateTime.now();
        this.chamado = chamado;
    }

    public Long getId() {
        return id;
    }

    public StatusChamado getStatusAnterior() {
        return statusAnterior;
    }

    public StatusChamado getStatusNovo() {
        return statusNovo;
    }

    public LocalDateTime getDataAlteracao() {
        return dataAlteracao;
    }

    public Chamado getChamado() {
        return chamado;
    }
}
