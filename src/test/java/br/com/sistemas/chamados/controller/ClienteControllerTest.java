package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@DisplayName("ClienteController")
class ClienteControllerTest {

    private static final Long ID = 1L;
    private static final String URL = "/clientes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private ChamadoService chamadoService;

    private final ClienteRequest requestValido =
            new ClienteRequest("Maria Silva", "maria@email.com", "47999999999");
    private final ClienteResponse responseEsperado =
            new ClienteResponse(ID, "Maria Silva", "maria@email.com", "47999999999");

    private String json(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /clientes")
    class Criar {

        @Test
        @DisplayName("retorna 201 com Location e corpo quando o request é válido")
        void retorna201() throws Exception {
            when(clienteService.criar(requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/clientes/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nome").value("Maria Silva"))
                    .andExpect(jsonPath("$.email").value("maria@email.com"))
                    .andExpect(jsonPath("$.telefone").value("47999999999"));
        }

        @Test
        @DisplayName("retorna 400 com detalhes quando nome e e-mail estão em branco")
        void retorna400QuandoCamposObrigatoriosVazios() throws Exception {
            String body = json(new ClienteRequest("", "", null));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                    .andExpect(jsonPath("$.detalhes", hasItems(
                            "nome: Nome é obrigatório",
                            "email: E-mail é obrigatório")));

            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("retorna 400 quando o e-mail é inválido")
        void retorna400QuandoEmailInvalido() throws Exception {
            String body = json(new ClienteRequest("Maria", "email-invalido", null));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("email: E-mail inválido"));

            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("retorna 400 quando nome e telefone excedem o tamanho máximo")
        void retorna400QuandoTamanhoExcedido() throws Exception {
            String body = json(new ClienteRequest("a".repeat(101), "maria@email.com", "1".repeat(21)));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes", hasItems(
                            "nome: Nome deve ter no máximo 100 caracteres",
                            "telefone: Telefone deve ter no máximo 20 caracteres")));

            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("retorna 409 quando o service lança RegraNegocioException")
        void retorna409QuandoEmailDuplicado() throws Exception {
            when(clienteService.criar(any(ClienteRequest.class)))
                    .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail maria@email.com"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value("Já existe cliente com o e-mail maria@email.com"));
        }

        @Test
        @DisplayName("retorna 400 quando o corpo é um JSON malformado")
        void retorna400QuandoJsonMalformado() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(clienteService);
        }
    }

    @Nested
    @DisplayName("GET /clientes")
    class Listar {

        @Test
        @DisplayName("retorna 200 com a lista de clientes")
        void retorna200ComLista() throws Exception {
            when(clienteService.listar()).thenReturn(List.of(
                    responseEsperado,
                    new ClienteResponse(2L, "João", "joao@email.com", null)));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].nome").value("João"));
        }

        @Test
        @DisplayName("retorna 200 com lista vazia")
        void retorna200ComListaVazia() throws Exception {
            when(clienteService.listar()).thenReturn(List.of());

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("GET /clientes/{id}")
    class Buscar {

        @Test
        @DisplayName("retorna 200 com o cliente")
        void retorna200() throws Exception {
            when(clienteService.buscar(ID)).thenReturn(responseEsperado);

            mockMvc.perform(get(URL + "/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("maria@email.com"));
        }

        @Test
        @DisplayName("retorna 404 quando o cliente não existe")
        void retorna404() throws Exception {
            when(clienteService.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

            mockMvc.perform(get(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.erro").value("Cliente 99 não encontrado"));
        }

        @Test
        @DisplayName("retorna 400 quando o id não é numérico")
        void retorna400QuandoIdInvalido() throws Exception {
            mockMvc.perform(get(URL + "/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(clienteService);
        }
    }

    @Nested
    @DisplayName("GET /clientes/{id}/chamados")
    class BuscarChamados {

        @Test
        @DisplayName("retorna 200 com a página de chamados e delega ao ChamadoService filtrando pelo cliente")
        void retorna200ComPagina() throws Exception {
            ChamadoResponse chamado = new ChamadoResponse(10L, "Impressora", "Não imprime",
                    LocalDate.of(2026, 1, 10), null, Prioridade.ALTA, StatusChamado.ABERTO,
                    responseEsperado, null, null);
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), eq(ID), isNull()))
                    .thenReturn(new PageImpl<>(List.of(chamado)));

            mockMvc.perform(get(URL + "/{id}/chamados", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].titulo").value("Impressora"));
        }

        @Test
        @DisplayName("aplica a paginação padrão: 20 itens ordenados por dataAbertura")
        void aplicaPaginacaoPadrao() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), eq(ID), isNull()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(URL + "/{id}/chamados", ID)).andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(chamadoService).listar(captor.capture(), isNull(), isNull(), eq(ID), isNull());
            assertThat(captor.getValue().getPageSize()).isEqualTo(20);
            assertThat(captor.getValue().getSort().getOrderFor("dataAbertura")).isNotNull();
        }

        @Test
        @DisplayName("retorna 404 quando o cliente não existe")
        void retorna404() throws Exception {
            when(chamadoService.listar(any(Pageable.class), isNull(), isNull(), eq(99L), isNull()))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

            mockMvc.perform(get(URL + "/{id}/chamados", 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /clientes/{id}")
    class Atualizar {

        @Test
        @DisplayName("retorna 200 com o cliente atualizado")
        void retorna200() throws Exception {
            when(clienteService.atualizar(ID, requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Maria Silva"));
        }

        @Test
        @DisplayName("retorna 400 e não chama o service quando o request é inválido")
        void retorna400QuandoRequestInvalido() throws Exception {
            String body = json(new ClienteRequest(" ", "maria@email.com", null));

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("nome: Nome é obrigatório"));

            verify(clienteService, never()).atualizar(any(), any());
        }

        @Test
        @DisplayName("retorna 404 quando o cliente não existe")
        void retorna404() throws Exception {
            when(clienteService.atualizar(eq(99L), any(ClienteRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

            mockMvc.perform(put(URL + "/{id}", 99L).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("retorna 409 quando o e-mail já pertence a outro cliente")
        void retorna409() throws Exception {
            when(clienteService.atualizar(eq(ID), any(ClienteRequest.class)))
                    .thenThrow(new RegraNegocioException("Já existe um cliente com o e-mail: maria@email.com"));

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /clientes/{id}")
    class Excluir {

        @Test
        @DisplayName("retorna 204 sem corpo e chama o service")
        void retorna204() throws Exception {
            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(clienteService).excluir(ID);
        }

        @Test
        @DisplayName("retorna 404 quando o cliente não existe")
        void retorna404() throws Exception {
            doThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"))
                    .when(clienteService).excluir(99L);

            mockMvc.perform(delete(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("retorna 409 quando o cliente possui chamados vinculados")
        void retorna409QuandoPossuiVinculo() throws Exception {
            doThrow(new DataIntegrityViolationException("fk"))
                    .when(clienteService).excluir(ID);

            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isConflict());
        }
    }
}
