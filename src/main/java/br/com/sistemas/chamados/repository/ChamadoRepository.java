package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    @Query("SELECT c FROM Chamado c WHERE (:status IS NULL OR c.status = :status) AND (:prioridade IS NULL OR c.prioridade = :prioridade) AND (:clienteId IS NULL OR c.cliente.id = :clienteId)")
    Page<Chamado> findAllOptionalByStatusAndPrioridadeAndClienteId(@Param("status") StatusChamado status, @Param("prioridade") Prioridade prioridade, @Param("clienteId") Long clienteId, Pageable paginacao);

    long countByStatus(StatusChamado status);

    long countByStatusAndDataAbertura(StatusChamado status, LocalDate dataAbertura);

    long countByDataFechamentoGreaterThanEqual(LocalDate data);

    long countByPrioridadeAndStatusNot(Prioridade prioridade, StatusChamado status);

    List<Chamado> findTop5ByOrderByIdDesc();
}
