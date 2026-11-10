package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.ChamadoAtribuirRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ChamadoStatusRequest;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.HistoricoStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChamadoService")
class ChamadoServiceTest {

    private static final Long ID = 1L;

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private TecnicoService tecnicoService;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private HistoricoStatusRepository historicoStatusRepository;

    @InjectMocks
    private ChamadoService chamadoService;

    private Cliente cliente;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        cliente = comId(new Cliente("Maria Silva", "maria@email.com", "47999999999"), 10L);
        categoria = comId(new Categoria("Hardware", "Problemas com equipamentos"), 20L);
    }

    private static <T> T comId(T entity, Long id) {
        // Os ids são gerados pelo banco e as entidades não expõem setter
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private Chamado chamadoComStatus(StatusChamado status) {
        Chamado chamado = comId(new Chamado("Impressora", "Não imprime", Prioridade.MEDIA, cliente, categoria), ID);
        chamado.setStatus(status);
        return chamado;
    }

    private ChamadoRequest requestValido() {
        return new ChamadoRequest("Monitor", "Tela piscando", Prioridade.ALTA, cliente.getId(), categoria.getId());
    }

    private HistoricoStatus historicoSalvo() {
        ArgumentCaptor<HistoricoStatus> captor = ArgumentCaptor.forClass(HistoricoStatus.class);
        verify(historicoStatusRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("salva o chamado como ABERTO, registra o histórico inicial e retorna o response")
        void salvaChamadoERegistraHistorico() {
            when(clienteService.buscarEntity(cliente.getId())).thenReturn(cliente);
            when(categoriaService.buscarEntity(categoria.getId())).thenReturn(categoria);
            when(chamadoRepository.save(any(Chamado.class))).thenAnswer(inv -> comId(inv.getArgument(0), ID));
            when(historicoStatusRepository.save(any(HistoricoStatus.class))).thenAnswer(returnsFirstArg());

            ChamadoResponse response = chamadoService.criar(requestValido());

            assertThat(response.id()).isEqualTo(ID);
            assertThat(response.titulo()).isEqualTo("Monitor");
            assertThat(response.descricao()).isEqualTo("Tela piscando");
            assertThat(response.prioridade()).isEqualTo(Prioridade.ALTA);
            assertThat(response.status()).isEqualTo(StatusChamado.ABERTO);
            assertThat(response.dataAbertura()).isEqualTo(LocalDate.now());
            assertThat(response.dataFechamento()).isNull();
            assertThat(response.tecnico()).isNull();
            assertThat(response.cliente().id()).isEqualTo(cliente.getId());
            assertThat(response.categoria().id()).isEqualTo(categoria.getId());

            ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
            verify(chamadoRepository).save(captor.capture());
            Chamado salvo = captor.getValue();
            assertThat(salvo.getCliente()).isSameAs(cliente);
            assertThat(salvo.getCategoria()).isSameAs(categoria);

            HistoricoStatus historico = historicoSalvo();
            assertThat(historico.getStatusAnterior()).isNull();
            assertThat(historico.getStatusNovo()).isEqualTo(StatusChamado.ABERTO);
            assertThat(historico.getChamado()).isSameAs(salvo);
            assertThat(salvo.getHistoricoStatus()).containsExactly(historico);
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException e não salva quando o cliente não existe")
        void falhaQuandoClienteNaoExiste() {
            when(clienteService.buscarEntity(cliente.getId()))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 10 não encontrado"));

            assertThatThrownBy(() -> chamadoService.criar(requestValido()))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Cliente 10 não encontrado");

            verify(chamadoRepository, never()).save(any());
            verifyNoInteractions(historicoStatusRepository);
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException e não salva quando a categoria não existe")
        void falhaQuandoCategoriaNaoExiste() {
            when(clienteService.buscarEntity(cliente.getId())).thenReturn(cliente);
            when(categoriaService.buscarEntity(categoria.getId()))
                    .thenThrow(new RecursoNaoEncontradoException("Categoria 20 não encontrado"));

            assertThatThrownBy(() -> chamadoService.criar(requestValido()))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Categoria 20 não encontrado");

            verify(chamadoRepository, never()).save(any());
            verifyNoInteractions(historicoStatusRepository);
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        private final Pageable paginacao = PageRequest.of(0, 20);

        @Test
        @DisplayName("valida o cliente e repassa todos os filtros ao repository quando clienteId é informado")
        void validaClienteQuandoClienteIdInformado() {
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            when(chamadoRepository.findAllOptionalByStatusAndPrioridadeAndClienteId(
                    StatusChamado.ABERTO, Prioridade.MEDIA, cliente.getId(), paginacao))
                    .thenReturn(new PageImpl<>(List.of(chamado), paginacao, 1));

            Page<ChamadoResponse> resultado =
                    chamadoService.listar(paginacao, StatusChamado.ABERTO, Prioridade.MEDIA, cliente.getId());

            verify(clienteService).buscarEntity(cliente.getId());
            assertThat(resultado.getTotalElements()).isEqualTo(1);
            assertThat(resultado.getContent()).singleElement()
                    .extracting(ChamadoResponse::id, ChamadoResponse::titulo)
                    .containsExactly(ID, "Impressora");
        }

        @Test
        @DisplayName("não consulta o ClienteService quando clienteId é nulo")
        void naoValidaClienteQuandoClienteIdNulo() {
            when(chamadoRepository.findAllOptionalByStatusAndPrioridadeAndClienteId(null, null, null, paginacao))
                    .thenReturn(new PageImpl<>(List.of(), paginacao, 0));

            Page<ChamadoResponse> resultado = chamadoService.listar(paginacao, null, null, null);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException e não consulta chamados quando o cliente não existe")
        void falhaQuandoClienteNaoExiste() {
            when(clienteService.buscarEntity(99L))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

            assertThatThrownBy(() -> chamadoService.listar(paginacao, null, null, 99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verifyNoInteractions(chamadoRepository);
        }
    }

    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("retorna o detalhe do chamado com comentários e histórico")
        void retornaDetalhe() {
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            chamado.adicionarHistoricoStatus(new HistoricoStatus(null, StatusChamado.ABERTO, chamado));
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));

            ChamadoDetalheResponse response = chamadoService.buscar(ID);

            assertThat(response.id()).isEqualTo(ID);
            assertThat(response.titulo()).isEqualTo("Impressora");
            assertThat(response.cliente().nome()).isEqualTo("Maria Silva");
            assertThat(response.categoria().nome()).isEqualTo("Hardware");
            assertThat(response.comentarios()).isEmpty();
            assertThat(response.historicoStatus()).singleElement()
                    .satisfies(h -> {
                        assertThat(h.statusAnterior()).isNull();
                        assertThat(h.statusNovo()).isEqualTo(StatusChamado.ABERTO);
                    });
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o chamado não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(chamadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chamadoService.buscar(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Chamado 1 não encontrado");
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("atualiza título, descrição, prioridade e cliente de um chamado aberto")
        void atualizaDados() {
            Cliente novoCliente = comId(new Cliente("João", "joao@email.com", null), 11L);
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            ChamadoRequest request = new ChamadoRequest("Monitor", "Tela piscando", Prioridade.CRITICA, 11L, categoria.getId());
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));
            when(clienteService.buscarEntity(11L)).thenReturn(novoCliente);

            ChamadoResponse response = chamadoService.atualizar(ID, request);

            assertThat(chamado.getTitulo()).isEqualTo("Monitor");
            assertThat(chamado.getDescricao()).isEqualTo("Tela piscando");
            assertThat(chamado.getPrioridade()).isEqualTo(Prioridade.CRITICA);
            assertThat(chamado.getCliente()).isSameAs(novoCliente);
            assertThat(response.titulo()).isEqualTo("Monitor");
            assertThat(response.prioridade()).isEqualTo(Prioridade.CRITICA);
            assertThat(response.cliente().id()).isEqualTo(11L);
            // A entidade é gerenciada pelo JPA (dirty checking), não precisa de save()
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança ChamadoFechadoException e não altera o chamado quando ele está fechado")
        void lancaExcecaoQuandoChamadoFechado() {
            Chamado chamado = chamadoComStatus(StatusChamado.FECHADO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));

            assertThatThrownBy(() -> chamadoService.atualizar(ID, requestValido()))
                    .isInstanceOf(ChamadoFechadoException.class)
                    .hasMessageContaining("fechado");

            assertThat(chamado.getTitulo()).isEqualTo("Impressora");
            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o chamado não existe")
        void lancaExcecaoQuandoChamadoNaoExiste() {
            when(chamadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chamadoService.atualizar(ID, requestValido()))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException quando o novo cliente não existe")
        void lancaExcecaoQuandoClienteNaoExiste() {
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));
            when(clienteService.buscarEntity(cliente.getId()))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 10 não encontrado"));

            assertThatThrownBy(() -> chamadoService.atualizar(ID, requestValido()))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Cliente 10 não encontrado");
        }
    }

    @Nested
    @DisplayName("atribuir")
    class Atribuir {

        @Test
        @DisplayName("atribui o técnico ao chamado e retorna o response")
        void atribuiTecnico() {
            Tecnico tecnico = comId(new Tecnico("Carlos", "carlos@empresa.com", Perfil.TECNICO), 30L);
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));
            when(tecnicoService.buscarEntity(30L)).thenReturn(tecnico);

            ChamadoResponse response = chamadoService.atribuir(ID, new ChamadoAtribuirRequest(30L));

            assertThat(chamado.getTecnico()).isSameAs(tecnico);
            assertThat(response.tecnico()).isNotNull();
            assertThat(response.tecnico().id()).isEqualTo(30L);
            assertThat(response.tecnico().nome()).isEqualTo("Carlos");
        }

        @Test
        @DisplayName("lança ChamadoFechadoException e não busca o técnico quando o chamado está fechado")
        void lancaExcecaoQuandoChamadoFechado() {
            Chamado chamado = chamadoComStatus(StatusChamado.FECHADO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));

            assertThatThrownBy(() -> chamadoService.atribuir(ID, new ChamadoAtribuirRequest(30L)))
                    .isInstanceOf(ChamadoFechadoException.class);

            assertThat(chamado.getTecnico()).isNull();
            verifyNoInteractions(tecnicoService);
        }

        @Test
        @DisplayName("propaga RecursoNaoEncontradoException quando o técnico não existe")
        void lancaExcecaoQuandoTecnicoNaoExiste() {
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));
            when(tecnicoService.buscarEntity(99L))
                    .thenThrow(new RecursoNaoEncontradoException("Tecnico 99 não encontrado"));

            assertThatThrownBy(() -> chamadoService.atribuir(ID, new ChamadoAtribuirRequest(99L)))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            assertThat(chamado.getTecnico()).isNull();
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o chamado não existe")
        void lancaExcecaoQuandoChamadoNaoExiste() {
            when(chamadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chamadoService.atribuir(ID, new ChamadoAtribuirRequest(30L)))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verifyNoInteractions(tecnicoService);
        }
    }

    @Nested
    @DisplayName("alterarStatus")
    class AlterarStatus {

        @Test
        @DisplayName("ao fechar, define dataFechamento como hoje e registra o histórico")
        void fechaChamado() {
            Chamado chamado = chamadoComStatus(StatusChamado.EM_ATENDIMENTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));
            when(historicoStatusRepository.save(any(HistoricoStatus.class))).thenAnswer(returnsFirstArg());

            ChamadoResponse response = chamadoService.alterarStatus(ID, new ChamadoStatusRequest(StatusChamado.FECHADO));

            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.FECHADO);
            assertThat(chamado.getDataFechamento()).isEqualTo(LocalDate.now());
            assertThat(response.status()).isEqualTo(StatusChamado.FECHADO);
            assertThat(response.dataFechamento()).isEqualTo(LocalDate.now());

            HistoricoStatus historico = historicoSalvo();
            assertThat(historico.getStatusAnterior()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
            assertThat(historico.getStatusNovo()).isEqualTo(StatusChamado.FECHADO);
            assertThat(chamado.getHistoricoStatus()).containsExactly(historico);
        }

        @Test
        @DisplayName("ao reabrir um chamado fechado, limpa dataFechamento e registra o histórico")
        void reabreChamado() {
            Chamado chamado = chamadoComStatus(StatusChamado.FECHADO);
            chamado.setDataFechamento(LocalDate.now().minusDays(1));
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));
            when(historicoStatusRepository.save(any(HistoricoStatus.class))).thenAnswer(returnsFirstArg());

            ChamadoResponse response = chamadoService.alterarStatus(ID, new ChamadoStatusRequest(StatusChamado.ABERTO));

            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
            assertThat(chamado.getDataFechamento()).isNull();
            assertThat(response.dataFechamento()).isNull();

            HistoricoStatus historico = historicoSalvo();
            assertThat(historico.getStatusAnterior()).isEqualTo(StatusChamado.FECHADO);
            assertThat(historico.getStatusNovo()).isEqualTo(StatusChamado.ABERTO);
        }

        @Test
        @DisplayName("entre status não finais, registra o histórico e não mexe em dataFechamento")
        void mudaStatusIntermediario() {
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));
            when(historicoStatusRepository.save(any(HistoricoStatus.class))).thenAnswer(returnsFirstArg());

            chamadoService.alterarStatus(ID, new ChamadoStatusRequest(StatusChamado.AGUARDANDO_CLIENTE));

            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.AGUARDANDO_CLIENTE);
            assertThat(chamado.getDataFechamento()).isNull();

            HistoricoStatus historico = historicoSalvo();
            assertThat(historico.getStatusAnterior()).isEqualTo(StatusChamado.ABERTO);
            assertThat(historico.getStatusNovo()).isEqualTo(StatusChamado.AGUARDANDO_CLIENTE);
        }

        @Test
        @DisplayName("não registra histórico quando o status informado é igual ao atual")
        void naoRegistraHistoricoQuandoStatusIgual() {
            Chamado chamado = chamadoComStatus(StatusChamado.EM_ATENDIMENTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));

            ChamadoResponse response = chamadoService.alterarStatus(ID, new ChamadoStatusRequest(StatusChamado.EM_ATENDIMENTO));

            assertThat(response.status()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
            assertThat(chamado.getHistoricoStatus()).isEmpty();
            verifyNoInteractions(historicoStatusRepository);
        }

        @Test
        @DisplayName("mantém a dataFechamento original quando um chamado já fechado é fechado novamente")
        void mantemDataFechamentoQuandoJaFechado() {
            LocalDate dataOriginal = LocalDate.now().minusDays(3);
            Chamado chamado = chamadoComStatus(StatusChamado.FECHADO);
            chamado.setDataFechamento(dataOriginal);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));

            chamadoService.alterarStatus(ID, new ChamadoStatusRequest(StatusChamado.FECHADO));

            assertThat(chamado.getDataFechamento()).isEqualTo(dataOriginal);
            verifyNoInteractions(historicoStatusRepository);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando o chamado não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(chamadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chamadoService.alterarStatus(ID, new ChamadoStatusRequest(StatusChamado.FECHADO)))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verifyNoInteractions(historicoStatusRepository);
        }
    }

    @Nested
    @DisplayName("excluir")
    class Excluir {

        @Test
        @DisplayName("remove o chamado quando ele existe")
        void removeChamadoExistente() {
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));

            chamadoService.excluir(ID);

            verify(chamadoRepository).delete(chamado);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException e não remove quando o chamado não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(chamadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chamadoService.excluir(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(chamadoRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("buscarEntity")
    class BuscarEntity {

        @Test
        @DisplayName("retorna a entidade quando o chamado existe")
        void retornaEntidade() {
            Chamado chamado = chamadoComStatus(StatusChamado.ABERTO);
            when(chamadoRepository.findById(ID)).thenReturn(Optional.of(chamado));

            assertThat(chamadoService.buscarEntity(ID)).isSameAs(chamado);
            verifyNoMoreInteractions(chamadoRepository);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException com o id na mensagem quando não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(chamadoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chamadoService.buscarEntity(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Chamado 99 não encontrado");
        }
    }
}
