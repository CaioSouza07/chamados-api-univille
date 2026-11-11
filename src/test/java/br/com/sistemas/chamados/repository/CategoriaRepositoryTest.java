package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Categoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("CategoriaRepository")
class CategoriaRepositoryTest {

    private static final String NOME = "Hardware";
    private static final String DESCRICAO = "Problemas com equipamentos";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Nested
    @DisplayName("operações herdadas do JpaRepository")
    class OperacoesBasicas {

        @Test
        @DisplayName("save gera id e persiste os dados da categoria")
        void salvaCategoriaGerandoId() {
            Categoria salva = categoriaRepository.saveAndFlush(new Categoria(NOME, DESCRICAO));
            entityManager.clear();

            assertThat(salva.getId()).isNotNull();
            assertThat(categoriaRepository.findById(salva.getId()))
                    .get()
                    .extracting(Categoria::getNome, Categoria::getDescricao)
                    .containsExactly(NOME, DESCRICAO);
        }

        @Test
        @DisplayName("findAll retorna todas as categorias persistidas")
        void listaTodasAsCategorias() {
            entityManager.persist(new Categoria("Hardware", "Equipamentos"));
            entityManager.persistAndFlush(new Categoria("Software", "Programas"));

            assertThat(categoriaRepository.findAll())
                    .extracting(Categoria::getNome)
                    .containsExactlyInAnyOrder("Hardware", "Software");
        }

        @Test
        @DisplayName("findById retorna vazio quando o id não existe")
        void retornaVazioQuandoIdNaoExiste() {
            assertThat(categoriaRepository.findById(999L)).isEmpty();
        }

        @Test
        @DisplayName("delete remove a categoria do banco")
        void removeCategoria() {
            Categoria categoria = entityManager.persistAndFlush(new Categoria(NOME, DESCRICAO));

            categoriaRepository.delete(categoria);
            categoriaRepository.flush();

            assertThat(categoriaRepository.findById(categoria.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("restrições da tabela")
    class Restricoes {

        @Test
        @DisplayName("rejeita categoria sem nome (not null)")
        void rejeitaNomeNulo() {
            assertThatThrownBy(() -> categoriaRepository.saveAndFlush(new Categoria(null, DESCRICAO)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita categoria sem descrição (not null)")
        void rejeitaDescricaoNula() {
            assertThatThrownBy(() -> categoriaRepository.saveAndFlush(new Categoria(NOME, null)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita nome maior que 50 caracteres")
        void rejeitaNomeMuitoLongo() {
            assertThatThrownBy(() -> categoriaRepository.saveAndFlush(new Categoria("a".repeat(51), DESCRICAO)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
