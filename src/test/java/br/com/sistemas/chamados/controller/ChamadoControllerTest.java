package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.dto.ChamadoAtribuirRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ChamadoStatusRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.dto.HistoricoStatusResponse;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.ParametroInvalidoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.service.ChamadoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChamadoController.class)
@DisplayName("ChamadoController")
class ChamadoControllerTest {

    private static final Long ID = 1L;
    private static final String URL = "/chamados";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ChamadoService chamadoService;

    private final ClienteResponse cliente = new ClienteResponse(10L, "Maria Silva", "maria@email.com", null);
    private final CategoriaResponse categoria = new CategoriaResponse(20L, "Hardware", "Equipamentos");
    private final TecnicoResponse tecnico = new TecnicoResponse(30L, "Carlos", "carlos@empresa.com", Perfil.TECNICO);

    private final ChamadoRequest requestValido =
            new ChamadoRequest("Impressora", "Não imprime", Prioridade.ALTA, 10L, 20L);

    private final ChamadoResponse responseEsperado = new ChamadoResponse(ID, "Impressora", "Não imprime",
            LocalDate.of(2026, 1, 10), null, Prioridade.ALTA, StatusChamado.ABERTO, cliente, null, categoria);

    private String json(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /chamados")
    class Criar {

        @Test
        @DisplayName("retorna 201 com Location e corpo quando o request é válido")
        void retorna201() throws Exception {
            when(chamadoService.criar(requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/chamados/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.titulo").value("Impressora"))
                    .andExpect(jsonPath("$.status").value("ABERTO"))
                    .andExpect(jsonPath("$.prioridade").value("ALTA"))
                    .andExpect(jsonPath("$.dataAbertura").value("2026-01-10"))
                    .andExpect(jsonPath("$.cliente.id").value(10))
                    .andExpect(jsonPath("$.categoria.nome").value("Hardware"))
                    .andExpect(jsonPath("$.tecnico").doesNotExist());
        }

        @Test
        @DisplayName("retorna 400 com detalhes quando todos os campos obrigatórios estão ausentes")
        void retorna400QuandoCamposObrigatoriosAusentes() throws Exception {
            String body = json(new ChamadoRequest("", "", null, null, null));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                    .andExpect(jsonPath("$.detalhes", hasItems(
                            "titulo: Título é obrigatório",
                            "descricao: Descrição é obrigatória",
                            "prioridade: Prioridade é obrigatória",
                            "clienteId: Cliente é obrigatório",
                            "categoriaId: Categoria é obrigatória")));

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 400 quando título e descrição excedem o tamanho máximo")
        void retorna400QuandoTamanhoExcedido() throws Exception {
            String body = json(new ChamadoRequest("t".repeat(101), "d".repeat(255), Prioridade.BAIXA, 10L, 20L));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes", hasItems(
                            "titulo: Título deve ter no máximo 100 caracteres",
                            "descricao: Descrição deve ter no máximo 254 caracteres")));

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 400 quando a prioridade não existe no enum")
        void retorna400QuandoPrioridadeInvalida() throws Exception {
            String body = """
                    {"titulo": "T", "descricao": "D", "prioridade": "URGENTISSIMA", "clienteId": 10, "categoriaId": 20}
                    """;

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                    .andExpect(jsonPath("$.detalhes[0]").value(
                            "prioridade: valor 'URGENTISSIMA' não aceito. Valores aceitos: BAIXA, MEDIA, ALTA, CRITICA"));

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 400 no formato padrão quando o JSON está mal formatado")
        void retorna400QuandoJsonMalFormatado() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("Corpo da requisição inválido ou mal formatado"));

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 404 quando o cliente ou a categoria não existem")
        void retorna404QuandoClienteNaoExiste() throws Exception {
            when(chamadoService.criar(any(ChamadoRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 10 não encontrado"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.erro").value("Cliente 10 não encontrado"));
        }
    }

    @Nested
    @DisplayName("GET /chamados")
    class Listar {

        @Test
        @DisplayName("retorna 200 com a página de chamados e sem filtros por padrão")
        void retorna200ComPagina() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(new PageImpl<>(List.of(responseEsperado)));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].titulo").value("Impressora"));
        }

        @Test
        @DisplayName("aplica a paginação padrão: 20 itens, mais recentes primeiro (dataAbertura e id decrescentes)")
        void aplicaPaginacaoPadrao() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(URL)).andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(chamadoService).listar(captor.capture(), isNull(), isNull(), isNull(), isNull());
            assertThat(captor.getValue().getPageNumber()).isZero();
            assertThat(captor.getValue().getPageSize()).isEqualTo(20);
            assertThat(captor.getValue().getSort().getOrderFor("dataAbertura").isDescending()).isTrue();
            assertThat(captor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
        }

        @Test
        @DisplayName("devolve a página no formato estável { content, page: { ... } }")
        void devolvePaginaEmFormatoEstavel() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(new PageImpl<>(List.of(responseEsperado), PageRequest.of(0, 20), 41));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(41))
                    .andExpect(jsonPath("$.page.totalPages").value(3))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.page.size").value(20));
        }

        @Test
        @DisplayName("repassa o texto de busca ao service")
        void repassaBusca() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), isNull(), eq("impressora")))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(URL).param("busca", "impressora"))
                    .andExpect(status().isOk());

            verify(chamadoService).listar(any(Pageable.class), isNull(), isNull(), isNull(), eq("impressora"));
        }

        @Test
        @DisplayName("retorna 400 quando a ordenação pedida não é aceita")
        void retorna400QuandoOrdenacaoInvalida() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), isNull(), isNull()))
                    .thenThrow(new ParametroInvalidoException("Não é possível ordenar por 'foo'"));

            mockMvc.perform(get(URL).param("sort", "foo"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Não é possível ordenar por 'foo'"));
        }

        @Test
        @DisplayName("repassa status, prioridade e clienteId ao service")
        void repassaFiltros() throws Exception {
            when(chamadoService.listar(any(Pageable.class), eq(StatusChamado.FECHADO), eq(Prioridade.CRITICA), eq(10L), isNull()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(URL)
                            .param("status", "FECHADO")
                            .param("prioridade", "CRITICA")
                            .param("clienteId", "10"))
                    .andExpect(status().isOk());

            verify(chamadoService).listar(any(Pageable.class), eq(StatusChamado.FECHADO), eq(Prioridade.CRITICA), eq(10L), isNull());
        }

        @Test
        @DisplayName("respeita os parâmetros page, size e sort informados")
        void respeitaParametrosDePaginacao() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(URL).param("page", "2").param("size", "5").param("sort", "titulo,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(chamadoService).listar(captor.capture(), isNull(), isNull(), isNull(), isNull());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(captor.getValue().getPageSize()).isEqualTo(5);
            assertThat(captor.getValue().getSort().getOrderFor("titulo").isDescending()).isTrue();
        }

        @Test
        @DisplayName("retorna 400 quando o status informado não existe no enum")
        void retorna400QuandoStatusInvalido() throws Exception {
            mockMvc.perform(get(URL).param("status", "INEXISTENTE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Parâmetro inválido"))
                    .andExpect(jsonPath("$.detalhes[0]").value(
                            "status: valor 'INEXISTENTE' não aceito. Valores aceitos: ABERTO, EM_ATENDIMENTO, AGUARDANDO_CLIENTE, FECHADO"));

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 404 quando o clienteId informado não existe")
        void retorna404QuandoClienteNaoExiste() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), eq(99L), isNull()))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

            mockMvc.perform(get(URL).param("clienteId", "99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /chamados/{id}")
    class Buscar {

        @Test
        @DisplayName("retorna 200 com o detalhe do chamado")
        void retorna200() throws Exception {
            ChamadoDetalheResponse detalhe = new ChamadoDetalheResponse(ID, "Impressora", "Não imprime",
                    LocalDate.of(2026, 1, 10), null, Prioridade.ALTA, StatusChamado.ABERTO,
                    cliente, tecnico, categoria, List.of(),
                    List.of(new HistoricoStatusResponse(5L, null, StatusChamado.ABERTO,
                            LocalDateTime.of(2026, 1, 10, 9, 0))));
            when(chamadoService.buscar(ID)).thenReturn(detalhe);

            mockMvc.perform(get(URL + "/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.tecnico.nome").value("Carlos"))
                    .andExpect(jsonPath("$.comentarios.length()").value(0))
                    .andExpect(jsonPath("$.historicoStatus.length()").value(1))
                    .andExpect(jsonPath("$.historicoStatus[0].statusNovo").value("ABERTO"));
        }

        @Test
        @DisplayName("retorna 404 quando o chamado não existe")
        void retorna404() throws Exception {
            when(chamadoService.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

            mockMvc.perform(get(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.erro").value("Chamado 99 não encontrado"));
        }

        @Test
        @DisplayName("retorna 400 quando o id não é numérico")
        void retorna400QuandoIdInvalido() throws Exception {
            mockMvc.perform(get(URL + "/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(chamadoService);
        }
    }

    @Nested
    @DisplayName("PUT /chamados/{id}")
    class Atualizar {

        @Test
        @DisplayName("retorna 200 com o chamado atualizado")
        void retorna200() throws Exception {
            when(chamadoService.atualizar(ID, requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Impressora"));
        }

        @Test
        @DisplayName("retorna 400 e não chama o service quando o request é inválido")
        void retorna400QuandoRequestInvalido() throws Exception {
            String body = json(new ChamadoRequest(" ", "Não imprime", Prioridade.ALTA, 10L, 20L));

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("titulo: Título é obrigatório"));

            verify(chamadoService, never()).atualizar(any(), any());
        }

        @Test
        @DisplayName("retorna 409 quando o chamado está fechado")
        void retorna409QuandoChamadoFechado() throws Exception {
            when(chamadoService.atualizar(eq(ID), any(ChamadoRequest.class)))
                    .thenThrow(new ChamadoFechadoException("Chamado está fechado, não é possível realizar qualquer alteração"));

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value("Chamado está fechado, não é possível realizar qualquer alteração"));
        }

        @Test
        @DisplayName("retorna 404 quando o chamado não existe")
        void retorna404() throws Exception {
            when(chamadoService.atualizar(eq(99L), any(ChamadoRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

            mockMvc.perform(put(URL + "/{id}", 99L).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /chamados/{id}/atribuir")
    class Atribuir {

        private final ChamadoAtribuirRequest request = new ChamadoAtribuirRequest(30L);

        @Test
        @DisplayName("retorna 200 com o chamado e o técnico atribuído")
        void retorna200() throws Exception {
            ChamadoResponse atribuido = new ChamadoResponse(ID, "Impressora", "Não imprime",
                    LocalDate.of(2026, 1, 10), null, Prioridade.ALTA, StatusChamado.ABERTO, cliente, tecnico, categoria);
            when(chamadoService.atribuir(ID, request)).thenReturn(atribuido);

            mockMvc.perform(patch(URL + "/{id}/atribuir", ID).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tecnico.id").value(30))
                    .andExpect(jsonPath("$.tecnico.perfil").value("TECNICO"));
        }

        @Test
        @DisplayName("retorna 400 quando tecnicoId está ausente")
        void retorna400QuandoTecnicoIdAusente() throws Exception {
            mockMvc.perform(patch(URL + "/{id}/atribuir", ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("tecnicoId: Técnico é obrigatório"));

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 409 quando o chamado está fechado")
        void retorna409QuandoChamadoFechado() throws Exception {
            when(chamadoService.atribuir(eq(ID), any(ChamadoAtribuirRequest.class)))
                    .thenThrow(new ChamadoFechadoException("Chamado está fechado, não é possível realizar qualquer alteração"));

            mockMvc.perform(patch(URL + "/{id}/atribuir", ID).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("retorna 404 quando o chamado ou o técnico não existem")
        void retorna404() throws Exception {
            when(chamadoService.atribuir(eq(ID), any(ChamadoAtribuirRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Técnico 30 não encontrado"));

            mockMvc.perform(patch(URL + "/{id}/atribuir", ID).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /chamados/{id}/status")
    class AlterarStatus {

        private final ChamadoStatusRequest request = new ChamadoStatusRequest(StatusChamado.FECHADO);

        @Test
        @DisplayName("retorna 200 com o chamado no novo status")
        void retorna200() throws Exception {
            ChamadoResponse fechado = new ChamadoResponse(ID, "Impressora", "Não imprime",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), Prioridade.ALTA, StatusChamado.FECHADO,
                    cliente, null, categoria);
            when(chamadoService.alterarStatus(ID, request)).thenReturn(fechado);

            mockMvc.perform(patch(URL + "/{id}/status", ID).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FECHADO"))
                    .andExpect(jsonPath("$.dataFechamento").value("2026-01-12"));
        }

        @Test
        @DisplayName("retorna 400 quando o status está ausente")
        void retorna400QuandoStatusAusente() throws Exception {
            mockMvc.perform(patch(URL + "/{id}/status", ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("status: Status é obrigatório"));

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 400 quando o status não existe no enum")
        void retorna400QuandoStatusInvalido() throws Exception {
            mockMvc.perform(patch(URL + "/{id}/status", ID).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"INEXISTENTE\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(chamadoService);
        }

        @Test
        @DisplayName("retorna 404 quando o chamado não existe")
        void retorna404() throws Exception {
            when(chamadoService.alterarStatus(eq(99L), any(ChamadoStatusRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

            mockMvc.perform(patch(URL + "/{id}/status", 99L).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /chamados/{id}")
    class Excluir {

        @Test
        @DisplayName("retorna 204 sem corpo e chama o service")
        void retorna204() throws Exception {
            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(chamadoService).excluir(ID);
        }

        @Test
        @DisplayName("retorna 404 quando o chamado não existe")
        void retorna404() throws Exception {
            doThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"))
                    .when(chamadoService).excluir(99L);

            mockMvc.perform(delete(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("retorna 409 quando há violação de integridade no banco")
        void retorna409QuandoViolacaoDeIntegridade() throws Exception {
            doThrow(new DataIntegrityViolationException("fk"))
                    .when(chamadoService).excluir(ID);

            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isConflict());
        }
    }
}
