package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService")
class ClienteServiceTest {

    private static final Long ID = 1L;
    private static final String NOME = "Maria Silva";
    private static final String EMAIL = "maria@email.com";
    private static final String TELEFONE = "47999999999";

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private static Cliente clienteComId(Long id, String nome, String email, String telefone) {
        Cliente cliente = new Cliente(nome, email, telefone);
        // O id é gerado pelo banco e a entidade não expõe setter
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        private final ClienteRequest request = new ClienteRequest(NOME, EMAIL, TELEFONE);

        @Test
        @DisplayName("salva o cliente e retorna o response quando o e-mail não existe")
        void salvaClienteQuandoEmailNaoExiste() {
            when(clienteRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class)))
                    .thenAnswer(inv -> {
                        Cliente c = inv.getArgument(0);
                        ReflectionTestUtils.setField(c, "id", ID);
                        return c;
                    });

            ClienteResponse response = clienteService.criar(request);

            assertThat(response).isEqualTo(new ClienteResponse(ID, NOME, EMAIL, TELEFONE));

            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            verify(clienteRepository).save(captor.capture());
            assertThat(captor.getValue())
                    .extracting(Cliente::getNome, Cliente::getEmail, Cliente::getTelefone)
                    .containsExactly(NOME, EMAIL, TELEFONE);
        }

        @Test
        @DisplayName("lança RegraNegocioException e não salva quando o e-mail já existe")
        void lancaExcecaoQuandoEmailJaExiste() {
            when(clienteRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> clienteService.criar(request))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining(EMAIL);

            verify(clienteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("converte todos os clientes para response")
        void retornaTodosOsClientes() {
            when(clienteRepository.findAll()).thenReturn(List.of(
                    clienteComId(1L, "Maria", "maria@email.com", "111"),
                    clienteComId(2L, "João", "joao@email.com", null)));

            List<ClienteResponse> resultado = clienteService.listar();

            assertThat(resultado).containsExactly(
                    new ClienteResponse(1L, "Maria", "maria@email.com", "111"),
                    new ClienteResponse(2L, "João", "joao@email.com", null));
        }

        @Test
        @DisplayName("retorna lista vazia quando não há clientes")
        void retornaListaVazia() {
            when(clienteRepository.findAll()).thenReturn(List.of());

            assertThat(clienteService.listar()).isEmpty();
        }
    }

    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("retorna o response quando o cliente existe")
        void retornaClienteExistente() {
            when(clienteRepository.findById(ID))
                    .thenReturn(Optional.of(clienteComId(ID, NOME, EMAIL, TELEFONE)));

            ClienteResponse response = clienteService.buscar(ID);

            assertThat(response).isEqualTo(new ClienteResponse(ID, NOME, EMAIL, TELEFONE));
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o cliente não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(clienteRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.buscar(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Cliente 1 não encontrado");
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        private final ClienteRequest request =
                new ClienteRequest("Maria Souza", "maria.souza@email.com", "47888888888");

        @Test
        @DisplayName("atualiza os dados do cliente quando o e-mail não pertence a outro cliente")
        void atualizaDadosDoCliente() {
            Cliente existente = clienteComId(ID, NOME, EMAIL, TELEFONE);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(existente));
            when(clienteRepository.existsByEmailAndIdNot(request.email(), ID)).thenReturn(false);

            ClienteResponse response = clienteService.atualizar(ID, request);

            assertThat(response)
                    .isEqualTo(new ClienteResponse(ID, "Maria Souza", "maria.souza@email.com", "47888888888"));
            assertThat(existente.getNome()).isEqualTo("Maria Souza");
            assertThat(existente.getEmail()).isEqualTo("maria.souza@email.com");
            assertThat(existente.getTelefone()).isEqualTo("47888888888");
            // A entidade é gerenciada pelo JPA (dirty checking), não precisa de save()
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o cliente não existe")
        void lancaExcecaoQuandoClienteNaoExiste() {
            when(clienteRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.atualizar(ID, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(clienteRepository, never()).existsByEmailAndIdNot(any(), any());
        }

        @Test
        @DisplayName("lança RegraNegocioException e não altera o cliente quando o e-mail é de outro cliente")
        void lancaExcecaoQuandoEmailPertenceAOutroCliente() {
            Cliente existente = clienteComId(ID, NOME, EMAIL, TELEFONE);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(existente));
            when(clienteRepository.existsByEmailAndIdNot(request.email(), ID)).thenReturn(true);

            assertThatThrownBy(() -> clienteService.atualizar(ID, request))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining(request.email());

            assertThat(existente)
                    .extracting(Cliente::getNome, Cliente::getEmail, Cliente::getTelefone)
                    .containsExactly(NOME, EMAIL, TELEFONE);
        }
    }

    @Nested
    @DisplayName("excluir")
    class Excluir {

        @Test
        @DisplayName("remove o cliente quando ele existe")
        void removeClienteExistente() {
            Cliente existente = clienteComId(ID, NOME, EMAIL, TELEFONE);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(existente));

            clienteService.excluir(ID);

            verify(clienteRepository).delete(existente);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException e não remove quando o cliente não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(clienteRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.excluir(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(clienteRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("buscarEntity")
    class BuscarEntity {

        @Test
        @DisplayName("retorna a entidade quando o cliente existe")
        void retornaEntidade() {
            Cliente existente = clienteComId(ID, NOME, EMAIL, TELEFONE);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(existente));

            assertThat(clienteService.buscarEntity(ID)).isSameAs(existente);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException com o id na mensagem quando não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.buscarEntity(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Cliente 99 não encontrado");
        }
    }
}
