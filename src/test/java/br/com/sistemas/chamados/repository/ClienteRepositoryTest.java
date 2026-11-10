package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Cliente;
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
@DisplayName("ClienteRepository")
class ClienteRepositoryTest {

    private static final String EMAIL = "maria@email.com";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente persistir(String nome, String email) {
        return entityManager.persistAndFlush(new Cliente(nome, email, "47999999999"));
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("retorna true quando já existe cliente com o e-mail")
        void retornaTrueQuandoEmailExiste() {
            persistir("Maria", EMAIL);

            assertThat(clienteRepository.existsByEmail(EMAIL)).isTrue();
        }

        @Test
        @DisplayName("retorna false quando não existe cliente com o e-mail")
        void retornaFalseQuandoEmailNaoExiste() {
            persistir("Maria", EMAIL);

            assertThat(clienteRepository.existsByEmail("outro@email.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByEmailAndIdNot")
    class ExistsByEmailAndIdNot {

        @Test
        @DisplayName("retorna false quando o e-mail pertence apenas ao próprio cliente")
        void retornaFalseQuandoEmailEDoProprioCliente() {
            Cliente maria = persistir("Maria", EMAIL);

            assertThat(clienteRepository.existsByEmailAndIdNot(EMAIL, maria.getId())).isFalse();
        }

        @Test
        @DisplayName("retorna true quando o e-mail pertence a outro cliente")
        void retornaTrueQuandoEmailEDeOutroCliente() {
            persistir("Maria", EMAIL);
            Cliente joao = persistir("João", "joao@email.com");

            assertThat(clienteRepository.existsByEmailAndIdNot(EMAIL, joao.getId())).isTrue();
        }

        @Test
        @DisplayName("retorna false quando nenhum cliente possui o e-mail")
        void retornaFalseQuandoEmailNaoExiste() {
            Cliente maria = persistir("Maria", EMAIL);

            assertThat(clienteRepository.existsByEmailAndIdNot("novo@email.com", maria.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("operações herdadas do JpaRepository")
    class OperacoesBasicas {

        @Test
        @DisplayName("save gera id e persiste os dados do cliente")
        void salvaClienteGerandoId() {
            Cliente salvo = clienteRepository.saveAndFlush(new Cliente("Maria", EMAIL, "47999999999"));
            entityManager.clear();

            assertThat(salvo.getId()).isNotNull();
            assertThat(clienteRepository.findById(salvo.getId()))
                    .get()
                    .extracting(Cliente::getNome, Cliente::getEmail, Cliente::getTelefone)
                    .containsExactly("Maria", EMAIL, "47999999999");
        }

        @Test
        @DisplayName("delete remove o cliente do banco")
        void removeCliente() {
            Cliente maria = persistir("Maria", EMAIL);

            clienteRepository.delete(maria);
            clienteRepository.flush();

            assertThat(clienteRepository.findById(maria.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("restrições da tabela")
    class Restricoes {

        @Test
        @DisplayName("rejeita dois clientes com o mesmo e-mail (unique)")
        void rejeitaEmailDuplicado() {
            persistir("Maria", EMAIL);

            assertThatThrownBy(() -> clienteRepository.saveAndFlush(new Cliente("Outra Maria", EMAIL, null)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejeita cliente sem nome (not null)")
        void rejeitaNomeNulo() {
            assertThatThrownBy(() -> clienteRepository.saveAndFlush(new Cliente(null, EMAIL, null)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
