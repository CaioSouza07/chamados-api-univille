package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    /**
     * Ordena a prioridade pela gravidade (BAIXA < MEDIA < ALTA < CRITICA). Como o enum é gravado
     * como texto, ordenar direto pela coluna seria alfabético (ALTA, BAIXA, CRITICA, MEDIA).
     */
    String GRAVIDADE_PRIORIDADE = "(CASE c.prioridade"
            + " WHEN br.com.sistemas.chamados.enums.Prioridade.CRITICA THEN 4"
            + " WHEN br.com.sistemas.chamados.enums.Prioridade.ALTA THEN 3"
            + " WHEN br.com.sistemas.chamados.enums.Prioridade.MEDIA THEN 2"
            + " ELSE 1 END)";

    /**
     * Lista chamados com filtros opcionais (null = não filtra).
     *
     * @param busca texto procurado no título ou no nome do cliente (sem diferenciar maiúsculas)
     */
    default Page<Chamado> pesquisar(StatusChamado status, Prioridade prioridade, Long clienteId, String busca, Pageable paginacao) {
        return pesquisarComFiltros(status, prioridade, clienteId, padraoLike(busca), ordenarPrioridadePorGravidade(paginacao));
    }

    // Carrega cliente, técnico e categoria na mesma consulta (evita uma consulta extra por chamado).
    @EntityGraph(attributePaths = {"cliente", "tecnico", "categoria"})
    @Query(value = """
            SELECT c FROM Chamado c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:prioridade IS NULL OR c.prioridade = :prioridade)
              AND (:clienteId IS NULL OR c.cliente.id = :clienteId)
              AND (:busca IS NULL OR LOWER(c.titulo) LIKE :busca ESCAPE '\\' OR LOWER(c.cliente.nome) LIKE :busca ESCAPE '\\')
            """,
            countQuery = """
            SELECT COUNT(c) FROM Chamado c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:prioridade IS NULL OR c.prioridade = :prioridade)
              AND (:clienteId IS NULL OR c.cliente.id = :clienteId)
              AND (:busca IS NULL OR LOWER(c.titulo) LIKE :busca ESCAPE '\\' OR LOWER(c.cliente.nome) LIKE :busca ESCAPE '\\')
            """)
    Page<Chamado> pesquisarComFiltros(@Param("status") StatusChamado status,
                                      @Param("prioridade") Prioridade prioridade,
                                      @Param("clienteId") Long clienteId,
                                      @Param("busca") String busca,
                                      Pageable paginacao);

    long countByStatus(StatusChamado status);

    long countByStatusAndDataAbertura(StatusChamado status, LocalDate dataAbertura);

    long countByDataFechamentoGreaterThanEqual(LocalDate data);

    long countByPrioridadeAndStatusNot(Prioridade prioridade, StatusChamado status);

    List<Chamado> findTop5ByOrderByIdDesc();

    /** "Impressora 50%" -> "%impressora 50\%%" (curingas digitados pelo usuário viram texto literal). */
    private static String padraoLike(String busca) {
        if (busca == null || busca.isBlank()) {
            return null;
        }
        String escapado = busca.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }

    private static Pageable ordenarPrioridadePorGravidade(Pageable paginacao) {
        if (paginacao.isUnpaged() || paginacao.getSort().getOrderFor("prioridade") == null) {
            return paginacao;
        }
        Sort ordenacao = Sort.unsorted();
        for (Sort.Order order : paginacao.getSort()) {
            ordenacao = ordenacao.and(order.getProperty().equals("prioridade")
                    ? JpaSort.unsafe(order.getDirection(), GRAVIDADE_PRIORIDADE)
                    : Sort.by(order));
        }
        return PageRequest.of(paginacao.getPageNumber(), paginacao.getPageSize(), ordenacao);
    }
}
