package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.TipoAutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("ComentarioRepository")
class ComentarioRepositoryTest {

    private static final String TEXTO = "Já tentei reiniciar o equipamento";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ComentarioRepository comentarioRepository;

    private Cliente cliente;
    private Tecnico tecnico;
    private Chamado chamado;

    @BeforeEach
    void setUp() {
        cliente = entityManager.persist(new Cliente("Maria", "maria@email.com", null));
        tecnico = entityManager.persist(new Tecnico("Carlos", "carlos@empresa.com", Perfil.TECNICO));
        Categoria categoria = entityManager.persist(new Categoria("Hardware", "Equipamentos"));
        chamado = entityManager.persistAndFlush(
                new Chamado("Impressora", "Não imprime", Prioridade.MEDIA, cliente, categoria));
    }

    @Nested
    @DisplayName("persistência")
    class Persistencia {

        @Test
        @DisplayName("save persiste comentário de CLIENTE com o cliente e sem técnico")
        void salvaComentarioDeCliente() {
            Comentario salvo = comentarioRepository.saveAndFlush(new Comentario(TEXTO, chamado, cliente));
            entityManager.clear();

            Comentario encontrado = comentarioRepository.findById(salvo.getId()).orElseThrow();
            assertThat(salvo.getId()).isNotNull();
            assertThat(encontrado.getTexto()).isEqualTo(TEXTO);
            assertThat(encontrado.getTipoAutor()).isEqualTo(TipoAutor.CLIENTE);
            assertThat(encontrado.getDataCriacao()).isNotNull();
            assertThat(encontrado.getCliente().getId()).isEqualTo(cliente.getId());
            assertThat(encontrado.getTecnico()).isNull();
            assertThat(encontrado.getChamado().getId()).isEqualTo(chamado.getId());
        }

        @Test
        @DisplayName("save persiste comentário de TECNICO com o técnico e sem cliente")
        void salvaComentarioDeTecnico() {
            Comentario salvo = comentarioRepository.saveAndFlush(new Comentario(TEXTO, chamado, tecnico));
            entityManager.clear();

            Comentario encontrado = comentarioRepository.findById(salvo.getId()).orElseThrow();
            assertThat(encontrado.getTipoAutor()).isEqualTo(TipoAutor.TECNICO);
            assertThat(encontrado.getTecnico().getId()).isEqualTo(tecnico.getId());
            assertThat(encontrado.getCliente()).isNull();
        }

        @Test
        @DisplayName("findById retorna vazio quando o id não existe")
        void retornaVazioQuandoIdNaoExiste() {
            assertThat(comentarioRepository.findById(999L)).isEmpty();
        }

        @Test
        @DisplayName("delete remove o comentário do banco")
        void removeComentario() {
            Comentario salvo = comentarioRepository.saveAndFlush(new Comentario(TEXTO, chamado, cliente));
            entityManager.clear();
            Comentario gerenciado = comentarioRepository.findById(salvo.getId()).orElseThrow();

            comentarioRepository.delete(gerenciado);
            comentarioRepository.flush();

            assertThat(comentarioRepository.findById(salvo.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("relacionamento com Chamado")
    class RelacionamentoComChamado {

        @Test
        @DisplayName("o chamado retorna seus comentários ordenados por dataCriacao crescente")
        void ordenaComentariosPorDataCriacao() {
            Comentario maisRecente = new Comentario("Segundo", chamado, tecnico);
            Comentario maisAntigo = new Comentario("Primeiro", chamado, cliente);
            ReflectionTestUtils.setField(maisRecente, "dataCriacao", LocalDateTime.of(2026, 1, 10, 11, 0));
            ReflectionTestUtils.setField(maisAntigo, "dataCriacao", LocalDateTime.of(2026, 1, 10, 9, 0));
            // Inserção propositalmente fora de ordem
            comentarioRepository.save(maisRecente);
            comentarioRepository.saveAndFlush(maisAntigo);
            entityManager.clear();

            Chamado encontrado = entityManager.find(Chamado.class, chamado.getId());

            assertThat(encontrado.getComentarios())
                    .extracting(Comentario::getTexto)
                    .containsExactly("Primeiro", "Segundo");
        }
    }

    @Nested
    @DisplayName("restrições da tabela")
    class Restricoes {

        @Test
        @DisplayName("rejeita comentário sem texto (not null)")
        void rejeitaTextoNulo() {
            Comentario comentario = new Comentario(null, chamado, cliente);

            assertThatThrownBy(() -> comentarioRepository.saveAndFlush(comentario))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita comentário sem chamado (not null)")
        void rejeitaChamadoNulo() {
            Comentario comentario = new Comentario(TEXTO, null, cliente);

            assertThatThrownBy(() -> comentarioRepository.saveAndFlush(comentario))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita texto maior que 500 caracteres")
        void rejeitaTextoMuitoLongo() {
            Comentario comentario = new Comentario("t".repeat(501), chamado, cliente);

            assertThatThrownBy(() -> comentarioRepository.saveAndFlush(comentario))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
