package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
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
@DisplayName("TecnicoRepository")
class TecnicoRepositoryTest {

    private static final String EMAIL = "carlos@empresa.com";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    private Tecnico persistir(String nome, String email) {
        return entityManager.persistAndFlush(new Tecnico(nome, email, Perfil.TECNICO));
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("retorna true quando já existe técnico com o e-mail")
        void retornaTrueQuandoEmailExiste() {
            persistir("Carlos", EMAIL);

            assertThat(tecnicoRepository.existsByEmail(EMAIL)).isTrue();
        }

        @Test
        @DisplayName("retorna false quando não existe técnico com o e-mail")
        void retornaFalseQuandoEmailNaoExiste() {
            persistir("Carlos", EMAIL);

            assertThat(tecnicoRepository.existsByEmail("outro@empresa.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByEmailAndIdNot")
    class ExistsByEmailAndIdNot {

        @Test
        @DisplayName("retorna false quando o e-mail pertence apenas ao próprio técnico")
        void retornaFalseQuandoEmailEDoProprioTecnico() {
            Tecnico carlos = persistir("Carlos", EMAIL);

            assertThat(tecnicoRepository.existsByEmailAndIdNot(EMAIL, carlos.getId())).isFalse();
        }

        @Test
        @DisplayName("retorna true quando o e-mail pertence a outro técnico")
        void retornaTrueQuandoEmailEDeOutroTecnico() {
            persistir("Carlos", EMAIL);
            Tecnico ana = persistir("Ana", "ana@empresa.com");

            assertThat(tecnicoRepository.existsByEmailAndIdNot(EMAIL, ana.getId())).isTrue();
        }

        @Test
        @DisplayName("retorna false quando nenhum técnico possui o e-mail")
        void retornaFalseQuandoEmailNaoExiste() {
            Tecnico carlos = persistir("Carlos", EMAIL);

            assertThat(tecnicoRepository.existsByEmailAndIdNot("novo@empresa.com", carlos.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("operações herdadas do JpaRepository")
    class OperacoesBasicas {

        @Test
        @DisplayName("save gera id e persiste os dados, incluindo o perfil")
        void salvaTecnicoGerandoId() {
            Tecnico salvo = tecnicoRepository.saveAndFlush(new Tecnico("Ana", "ana@empresa.com", Perfil.ADMIN));
            entityManager.clear();

            assertThat(salvo.getId()).isNotNull();
            assertThat(tecnicoRepository.findById(salvo.getId()))
                    .get()
                    .extracting(Tecnico::getNome, Tecnico::getEmail, Tecnico::getPerfil)
                    .containsExactly("Ana", "ana@empresa.com", Perfil.ADMIN);
        }

        @Test
        @DisplayName("delete remove o técnico do banco")
        void removeTecnico() {
            Tecnico carlos = persistir("Carlos", EMAIL);

            tecnicoRepository.delete(carlos);
            tecnicoRepository.flush();

            assertThat(tecnicoRepository.findById(carlos.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("restrições da tabela")
    class Restricoes {

        @Test
        @DisplayName("rejeita dois técnicos com o mesmo e-mail (unique)")
        void rejeitaEmailDuplicado() {
            persistir("Carlos", EMAIL);

            assertThatThrownBy(() -> tecnicoRepository.saveAndFlush(new Tecnico("Outro", EMAIL, Perfil.ADMIN)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita técnico sem nome (not null)")
        void rejeitaNomeNulo() {
            assertThatThrownBy(() -> tecnicoRepository.saveAndFlush(new Tecnico(null, EMAIL, Perfil.TECNICO)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita técnico sem perfil (not null)")
        void rejeitaPerfilNulo() {
            assertThatThrownBy(() -> tecnicoRepository.saveAndFlush(new Tecnico("Carlos", EMAIL, null)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
