package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("ChamadoRepository")
class ChamadoRepositoryTest {

    private static final Pageable PAGINA = PageRequest.of(0, 20, Sort.by("titulo"));

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChamadoRepository chamadoRepository;

    private Cliente maria;
    private Cliente joao;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        maria = entityManager.persist(new Cliente("Maria", "maria@email.com", null));
        joao = entityManager.persist(new Cliente("João", "joao@email.com", null));
        categoria = entityManager.persist(new Categoria("Hardware", "Equipamentos"));
    }

    private Chamado persistir(String titulo, Prioridade prioridade, StatusChamado status, Cliente cliente) {
        Chamado chamado = new Chamado(titulo, "Descrição de " + titulo, prioridade, cliente, categoria);
        chamado.setStatus(status);
        return entityManager.persistAndFlush(chamado);
    }

    @Nested
    @DisplayName("findAllOptionalByStatusAndPrioridadeAndClienteId")
    class FindAllOptional {

        @BeforeEach
        void carregarChamados() {
            persistir("A - Impressora", Prioridade.ALTA, StatusChamado.ABERTO, maria);
            persistir("B - Monitor", Prioridade.BAIXA, StatusChamado.ABERTO, joao);
            persistir("C - Teclado", Prioridade.ALTA, StatusChamado.FECHADO, maria);
            persistir("D - Mouse", Prioridade.MEDIA, StatusChamado.EM_ATENDIMENTO, joao);
            entityManager.clear();
        }

        private Page<Chamado> buscar(StatusChamado status, Prioridade prioridade, Long clienteId) {
            return chamadoRepository.findAllOptionalByStatusAndPrioridadeAndClienteId(status, prioridade, clienteId, PAGINA);
        }

        @Test
        @DisplayName("sem filtros, retorna todos os chamados")
        void semFiltros() {
            assertThat(buscar(null, null, null).getContent())
                    .extracting(Chamado::getTitulo)
                    .containsExactly("A - Impressora", "B - Monitor", "C - Teclado", "D - Mouse");
        }

        @Test
        @DisplayName("filtra apenas por status")
        void filtraPorStatus() {
            assertThat(buscar(StatusChamado.ABERTO, null, null).getContent())
                    .extracting(Chamado::getTitulo)
                    .containsExactly("A - Impressora", "B - Monitor");
        }

        @Test
        @DisplayName("filtra apenas por prioridade")
        void filtraPorPrioridade() {
            assertThat(buscar(null, Prioridade.ALTA, null).getContent())
                    .extracting(Chamado::getTitulo)
                    .containsExactly("A - Impressora", "C - Teclado");
        }

        @Test
        @DisplayName("filtra apenas por cliente")
        void filtraPorCliente() {
            assertThat(buscar(null, null, joao.getId()).getContent())
                    .extracting(Chamado::getTitulo)
                    .containsExactly("B - Monitor", "D - Mouse");
        }

        @Test
        @DisplayName("combina status, prioridade e cliente")
        void combinaFiltros() {
            assertThat(buscar(StatusChamado.ABERTO, Prioridade.ALTA, maria.getId()).getContent())
                    .extracting(Chamado::getTitulo)
                    .containsExactly("A - Impressora");
        }

        @Test
        @DisplayName("retorna página vazia quando nenhum chamado atende aos filtros")
        void semResultados() {
            Page<Chamado> pagina = buscar(StatusChamado.FECHADO, Prioridade.BAIXA, joao.getId());

            assertThat(pagina.getContent()).isEmpty();
            assertThat(pagina.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("respeita tamanho, número da página e ordenação")
        void respeitaPaginacaoEOrdenacao() {
            Pageable segundaPagina = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "titulo"));

            Page<Chamado> pagina = chamadoRepository
                    .findAllOptionalByStatusAndPrioridadeAndClienteId(null, null, null, segundaPagina);

            assertThat(pagina.getTotalElements()).isEqualTo(4);
            assertThat(pagina.getTotalPages()).isEqualTo(2);
            assertThat(pagina.getContent())
                    .extracting(Chamado::getTitulo)
                    .containsExactly("A - Impressora");
        }
    }

    @Nested
    @DisplayName("persistência e relacionamentos")
    class Persistencia {

        @Test
        @DisplayName("save gera id e persiste os dados e relacionamentos do chamado")
        void salvaChamado() {
            Tecnico tecnico = entityManager.persist(new Tecnico("Carlos", "carlos@empresa.com", Perfil.TECNICO));
            Chamado chamado = new Chamado("Impressora", "Não imprime", Prioridade.ALTA, maria, categoria);
            chamado.setTecnico(tecnico);

            Chamado salvo = chamadoRepository.saveAndFlush(chamado);
            entityManager.clear();

            assertThat(salvo.getId()).isNotNull();
            Chamado encontrado = chamadoRepository.findById(salvo.getId()).orElseThrow();
            assertThat(encontrado.getTitulo()).isEqualTo("Impressora");
            assertThat(encontrado.getStatus()).isEqualTo(StatusChamado.ABERTO);
            assertThat(encontrado.getPrioridade()).isEqualTo(Prioridade.ALTA);
            assertThat(encontrado.getDataFechamento()).isNull();
            assertThat(encontrado.getCliente().getId()).isEqualTo(maria.getId());
            assertThat(encontrado.getCategoria().getId()).isEqualTo(categoria.getId());
            assertThat(encontrado.getTecnico().getId()).isEqualTo(tecnico.getId());
        }

        @Test
        @DisplayName("persiste em cascata o histórico de status e os comentários adicionados ao chamado")
        void persisteEmCascata() {
            Chamado chamado = new Chamado("Impressora", "Não imprime", Prioridade.ALTA, maria, categoria);
            chamado.adicionarHistoricoStatus(new HistoricoStatus(null, StatusChamado.ABERTO, chamado));
            chamado.adicionarComentario(new Comentario("Já tentei reiniciar", chamado, maria));

            Chamado salvo = chamadoRepository.saveAndFlush(chamado);
            entityManager.clear();

            Chamado encontrado = chamadoRepository.findById(salvo.getId()).orElseThrow();
            assertThat(encontrado.getHistoricoStatus()).singleElement()
                    .satisfies(h -> assertThat(h.getStatusNovo()).isEqualTo(StatusChamado.ABERTO));
            assertThat(encontrado.getComentarios()).singleElement()
                    .satisfies(c -> assertThat(c.getTexto()).isEqualTo("Já tentei reiniciar"));
        }

        @Test
        @DisplayName("delete remove o chamado e, em cascata, seu histórico e comentários")
        void removeChamadoEmCascata() {
            Chamado chamado = new Chamado("Impressora", "Não imprime", Prioridade.ALTA, maria, categoria);
            chamado.adicionarHistoricoStatus(new HistoricoStatus(null, StatusChamado.ABERTO, chamado));
            chamado.adicionarComentario(new Comentario("Já tentei reiniciar", chamado, maria));
            chamadoRepository.saveAndFlush(chamado);
            Long historicoId = chamado.getHistoricoStatus().get(0).getId();
            Long comentarioId = chamado.getComentarios().get(0).getId();

            chamadoRepository.delete(chamado);
            chamadoRepository.flush();
            entityManager.clear();

            assertThat(chamadoRepository.findById(chamado.getId())).isEmpty();
            assertThat(entityManager.find(HistoricoStatus.class, historicoId)).isNull();
            assertThat(entityManager.find(Comentario.class, comentarioId)).isNull();
        }
    }

    @Nested
    @DisplayName("restrições da tabela")
    class Restricoes {

        @Test
        @DisplayName("rejeita chamado sem título (not null)")
        void rejeitaTituloNulo() {
            Chamado chamado = new Chamado(null, "Descrição", Prioridade.BAIXA, maria, categoria);

            assertThatThrownBy(() -> chamadoRepository.saveAndFlush(chamado))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita chamado sem cliente (not null)")
        void rejeitaClienteNulo() {
            Chamado chamado = new Chamado("Título", "Descrição", Prioridade.BAIXA, null, categoria);

            assertThatThrownBy(() -> chamadoRepository.saveAndFlush(chamado))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita chamado sem categoria (not null)")
        void rejeitaCategoriaNula() {
            Chamado chamado = new Chamado("Título", "Descrição", Prioridade.BAIXA, maria, null);

            assertThatThrownBy(() -> chamadoRepository.saveAndFlush(chamado))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita chamado sem prioridade (not null)")
        void rejeitaPrioridadeNula() {
            Chamado chamado = new Chamado("Título", "Descrição", null, maria, categoria);

            assertThatThrownBy(() -> chamadoRepository.saveAndFlush(chamado))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
