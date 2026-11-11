package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.TecnicoRepository;
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
@DisplayName("TecnicoService")
class TecnicoServiceTest {

    private static final Long ID = 1L;
    private static final String NOME = "Carlos Souza";
    private static final String EMAIL = "carlos@empresa.com";
    private static final Perfil PERFIL = Perfil.TECNICO;

    @Mock
    private TecnicoRepository tecnicoRepository;

    @InjectMocks
    private TecnicoService tecnicoService;

    private static Tecnico tecnicoComId(Long id, String nome, String email, Perfil perfil) {
        Tecnico tecnico = new Tecnico(nome, email, perfil);
        // O id é gerado pelo banco e a entidade não expõe setter
        ReflectionTestUtils.setField(tecnico, "id", id);
        return tecnico;
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        private final TecnicoRequest request = new TecnicoRequest(NOME, EMAIL, PERFIL);

        @Test
        @DisplayName("salva o técnico e retorna o response quando o e-mail não existe")
        void salvaTecnicoQuandoEmailNaoExiste() {
            when(tecnicoRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(tecnicoRepository.save(any(Tecnico.class)))
                    .thenAnswer(inv -> {
                        Tecnico t = inv.getArgument(0);
                        ReflectionTestUtils.setField(t, "id", ID);
                        return t;
                    });

            TecnicoResponse response = tecnicoService.criar(request);

            assertThat(response).isEqualTo(new TecnicoResponse(ID, NOME, EMAIL, PERFIL));

            ArgumentCaptor<Tecnico> captor = ArgumentCaptor.forClass(Tecnico.class);
            verify(tecnicoRepository).save(captor.capture());
            assertThat(captor.getValue())
                    .extracting(Tecnico::getNome, Tecnico::getEmail, Tecnico::getPerfil)
                    .containsExactly(NOME, EMAIL, PERFIL);
        }

        @Test
        @DisplayName("lança RegraNegocioException e não salva quando o e-mail já existe")
        void lancaExcecaoQuandoEmailJaExiste() {
            when(tecnicoRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> tecnicoService.criar(request))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining(EMAIL);

            verify(tecnicoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("converte todos os técnicos para response")
        void retornaTodosOsTecnicos() {
            when(tecnicoRepository.findAll()).thenReturn(List.of(
                    tecnicoComId(1L, "Carlos", "carlos@empresa.com", Perfil.TECNICO),
                    tecnicoComId(2L, "Ana", "ana@empresa.com", Perfil.ADMIN)));

            assertThat(tecnicoService.listar()).containsExactly(
                    new TecnicoResponse(1L, "Carlos", "carlos@empresa.com", Perfil.TECNICO),
                    new TecnicoResponse(2L, "Ana", "ana@empresa.com", Perfil.ADMIN));
        }

        @Test
        @DisplayName("retorna lista vazia quando não há técnicos")
        void retornaListaVazia() {
            when(tecnicoRepository.findAll()).thenReturn(List.of());

            assertThat(tecnicoService.listar()).isEmpty();
        }
    }

    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("retorna o response quando o técnico existe")
        void retornaTecnicoExistente() {
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.of(tecnicoComId(ID, NOME, EMAIL, PERFIL)));

            assertThat(tecnicoService.buscar(ID)).isEqualTo(new TecnicoResponse(ID, NOME, EMAIL, PERFIL));
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o técnico não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tecnicoService.buscar(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Tecnico 1 não encontrado");
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        private final TecnicoRequest request = new TecnicoRequest("Carlos Silva", "carlos.silva@empresa.com", Perfil.ADMIN);

        @Test
        @DisplayName("atualiza os dados do técnico quando o e-mail não pertence a outro técnico")
        void atualizaDadosDoTecnico() {
            Tecnico existente = tecnicoComId(ID, NOME, EMAIL, PERFIL);
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.of(existente));
            when(tecnicoRepository.existsByEmailAndIdNot(request.email(), ID)).thenReturn(false);

            TecnicoResponse response = tecnicoService.atualizar(ID, request);

            assertThat(response).isEqualTo(new TecnicoResponse(ID, "Carlos Silva", "carlos.silva@empresa.com", Perfil.ADMIN));
            assertThat(existente)
                    .extracting(Tecnico::getNome, Tecnico::getEmail, Tecnico::getPerfil)
                    .containsExactly("Carlos Silva", "carlos.silva@empresa.com", Perfil.ADMIN);
            // A entidade é gerenciada pelo JPA (dirty checking), não precisa de save()
            verify(tecnicoRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o técnico não existe")
        void lancaExcecaoQuandoTecnicoNaoExiste() {
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tecnicoService.atualizar(ID, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(tecnicoRepository, never()).existsByEmailAndIdNot(any(), any());
        }

        @Test
        @DisplayName("lança RegraNegocioException e não altera o técnico quando o e-mail é de outro técnico")
        void lancaExcecaoQuandoEmailPertenceAOutroTecnico() {
            Tecnico existente = tecnicoComId(ID, NOME, EMAIL, PERFIL);
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.of(existente));
            when(tecnicoRepository.existsByEmailAndIdNot(request.email(), ID)).thenReturn(true);

            assertThatThrownBy(() -> tecnicoService.atualizar(ID, request))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining(request.email());

            assertThat(existente)
                    .extracting(Tecnico::getNome, Tecnico::getEmail, Tecnico::getPerfil)
                    .containsExactly(NOME, EMAIL, PERFIL);
        }
    }

    @Nested
    @DisplayName("excluir")
    class Excluir {

        @Test
        @DisplayName("remove o técnico quando ele existe")
        void removeTecnicoExistente() {
            Tecnico existente = tecnicoComId(ID, NOME, EMAIL, PERFIL);
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.of(existente));

            tecnicoService.excluir(ID);

            verify(tecnicoRepository).delete(existente);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException e não remove quando o técnico não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tecnicoService.excluir(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(tecnicoRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("buscarEntity")
    class BuscarEntity {

        @Test
        @DisplayName("retorna a entidade quando o técnico existe")
        void retornaEntidade() {
            Tecnico existente = tecnicoComId(ID, NOME, EMAIL, PERFIL);
            when(tecnicoRepository.findById(ID)).thenReturn(Optional.of(existente));

            assertThat(tecnicoService.buscarEntity(ID)).isSameAs(existente);
            verifyNoMoreInteractions(tecnicoRepository);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException com o id na mensagem quando não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(tecnicoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tecnicoService.buscarEntity(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Tecnico 99 não encontrado");
        }
    }
}
