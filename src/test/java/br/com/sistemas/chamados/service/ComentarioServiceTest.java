package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.enums.TipoAutor;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComentarioService")
class ComentarioServiceTest {

    private static final Long CHAMADO_ID = 1L;
    private static final Long COMENTARIO_ID = 100L;
    private static final String TEXTO = "Já tentei reiniciar o equipamento";

    @Mock
    private ComentarioRepository comentarioRepository;

    @Mock
    private ChamadoService chamadoService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private TecnicoService tecnicoService;

    @InjectMocks
    private ComentarioService comentarioService;

    private Cliente cliente;
    private Tecnico tecnico;
    private Chamado chamado;

    @BeforeEach
    void setUp() {
        cliente = comId(new Cliente("Maria Silva", "maria@email.com", null), 10L);
        tecnico = comId(new Tecnico("Carlos", "carlos@empresa.com", Perfil.TECNICO), 30L);
        Categoria categoria = comId(new Categoria("Hardware", "Equipamentos"), 20L);
        chamado = comId(new Chamado("Impressora", "Não imprime", Prioridade.MEDIA, cliente, categoria), CHAMADO_ID);
    }

    private static <T> T comId(T entity, Long id) {
        // Os ids são gerados pelo banco e as entidades não expõem setter
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private void simularSalvarComentario() {
        when(comentarioRepository.save(any(Comentario.class)))
                .thenAnswer(inv -> comId(inv.getArgument(0), COMENTARIO_ID));
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("cria comentário de CLIENTE, vincula ao chamado e retorna o response")
        void criaComentarioDeCliente() {
            when(chamadoService.buscarEntity(CHAMADO_ID)).thenReturn(chamado);
            when(clienteService.buscarEntity(10L)).thenReturn(cliente);
            simularSalvarComentario();

            ComentarioResponse response =
                    comentarioService.criar(CHAMADO_ID, new ComentarioRequest(TEXTO, TipoAutor.CLIENTE, 10L));

            assertThat(response.id()).isEqualTo(COMENTARIO_ID);
            assertThat(response.texto()).isEqualTo(TEXTO);
            assertThat(response.tipoAutor()).isEqualTo(TipoAutor.CLIENTE);
            assertThat(response.autorId()).isEqualTo(10L);
            assertThat(response.autorNome()).isEqualTo("Maria Silva");
            assertThat(response.dataCriacao()).isNotNull();

            ArgumentCaptor<Comentario> captor = ArgumentCaptor.forClass(Comentario.class);
            verify(comentarioRepository).save(captor.capture());
            Comentario salvo = captor.getValue();
            assertThat(salvo.getCliente()).isSameAs(cliente);
            assertThat(salvo.getTecnico()).isNull();
            assertThat(salvo.getChamado()).isSameAs(chamado);
            assertThat(chamado.getComentarios()).containsExactly(salvo);
            verifyNoInteractions(tecnicoService);
        }

        @Test
        @DisplayName("cria comentário de TECNICO, vincula ao chamado e retorna o response")
        void criaComentarioDeTecnico() {
            when(chamadoService.buscarEntity(CHAMADO_ID)).thenReturn(chamado);
            when(tecnicoService.buscarEntity(30L)).thenReturn(tecnico);
            simularSalvarComentario();

            ComentarioResponse response =
                    comentarioService.criar(CHAMADO_ID, new ComentarioRequest(TEXTO, TipoAutor.TECNICO, 30L));

            assertThat(response.id()).isEqualTo(COMENTARIO_ID);
            assertThat(response.tipoAutor()).isEqualTo(TipoAutor.TECNICO);
            assertThat(response.autorId()).isEqualTo(30L);
            assertThat(response.autorNome()).isEqualTo("Carlos");

            ArgumentCaptor<Comentario> captor = ArgumentCaptor.forClass(Comentario.class);
            verify(comentarioRepository).save(captor.capture());
            Comentario salvo = captor.getValue();
            assertThat(salvo.getTecnico()).isSameAs(tecnico);
            assertThat(salvo.getCliente()).isNull();
            assertThat(chamado.getComentarios()).containsExactly(salvo);
            verifyNoInteractions(clienteService);
        }

        @ParameterizedTest(name = "permite comentar em chamado com status {0}")
        @EnumSource(value = StatusChamado.class, names = "FECHADO", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("permite comentar em chamado que não está fechado")
        void permiteComentarEmChamadoNaoFechado(StatusChamado statusChamado) {
            chamado.setStatus(statusChamado);
            when(chamadoService.buscarEntity(CHAMADO_ID)).thenReturn(chamado);
            when(clienteService.buscarEntity(10L)).thenReturn(cliente);
            simularSalvarComentario();

            ComentarioResponse response =
                    comentarioService.criar(CHAMADO_ID, new ComentarioRequest(TEXTO, TipoAutor.CLIENTE, 10L));

            assertThat(response.id()).isEqualTo(COMENTARIO_ID);
        }

        @Test
        @DisplayName("lança ChamadoFechadoException e não busca o autor nem salva quando o chamado está fechado")
        void lancaExcecaoQuandoChamadoFechado() {
            chamado.setStatus(StatusChamado.FECHADO);
            when(chamadoService.buscarEntity(CHAMADO_ID)).thenReturn(chamado);

            assertThatThrownBy(() ->
                    comentarioService.criar(CHAMADO_ID, new ComentarioRequest(TEXTO, TipoAutor.CLIENTE, 10L)))
                    .isInstanceOf(ChamadoFechadoException.class)
                    .hasMessageContaining("fechado");

            verify(comentarioRepository, never()).save(any());
            verifyNoInteractions(clienteService, tecnicoService);
            assertThat(chamado.getComentarios()).isEmpty();
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException quando o chamado não existe")
        void lancaExcecaoQuandoChamadoNaoExiste() {
            when(chamadoService.buscarEntity(CHAMADO_ID))
                    .thenThrow(new RecursoNaoEncontradoException("Chamado 1 não encontrado"));

            assertThatThrownBy(() ->
                    comentarioService.criar(CHAMADO_ID, new ComentarioRequest(TEXTO, TipoAutor.CLIENTE, 10L)))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Chamado 1 não encontrado");

            verifyNoInteractions(comentarioRepository, clienteService, tecnicoService);
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException e não salva quando o cliente autor não existe")
        void lancaExcecaoQuandoClienteAutorNaoExiste() {
            when(chamadoService.buscarEntity(CHAMADO_ID)).thenReturn(chamado);
            when(clienteService.buscarEntity(99L))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

            assertThatThrownBy(() ->
                    comentarioService.criar(CHAMADO_ID, new ComentarioRequest(TEXTO, TipoAutor.CLIENTE, 99L)))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Cliente 99 não encontrado");

            verify(comentarioRepository, never()).save(any());
            assertThat(chamado.getComentarios()).isEmpty();
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException e não salva quando o técnico autor não existe")
        void lancaExcecaoQuandoTecnicoAutorNaoExiste() {
            when(chamadoService.buscarEntity(CHAMADO_ID)).thenReturn(chamado);
            when(tecnicoService.buscarEntity(99L))
                    .thenThrow(new RecursoNaoEncontradoException("Tecnico 99 não encontrado"));

            assertThatThrownBy(() ->
                    comentarioService.criar(CHAMADO_ID, new ComentarioRequest(TEXTO, TipoAutor.TECNICO, 99L)))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Tecnico 99 não encontrado");

            verify(comentarioRepository, never()).save(any());
            assertThat(chamado.getComentarios()).isEmpty();
        }
    }
}
