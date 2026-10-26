package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    @Query("SELECT c FROM Chamado c  WHERE (:status IS NULL OR c.status = :status) AND (:prioridade IS NULL OR c.prioridade = :prioridade) AND (:clienteId IS NULL OR c.cliente = :cliente)")
    Page<Chamado> findAllOptionalByStatusAndPrioridadeAndClienteId(@Param("status") StatusChamado status, @Param("prioridade") Prioridade prioridade, @Param("cliente") Cliente cliente, Pageable paginacao);
}
