package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(TecnicoController.class)
@DisplayName("TecnicoController")
class TecnicoControllerTest {

    private static final Long ID = 1L;
    private static final String URL = "/tecnicos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private TecnicoService tecnicoService;

    private final TecnicoRequest requestValido = new TecnicoRequest("Carlos Souza", "carlos@empresa.com", Perfil.TECNICO);
    private final TecnicoResponse responseEsperado = new TecnicoResponse(ID, "Carlos Souza", "carlos@empresa.com", Perfil.TECNICO);

    private String json(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /tecnicos")
    class Criar {

        @Test
        @DisplayName("retorna 201 com Location e corpo quando o request é válido")
        void retorna201() throws Exception {
            when(tecnicoService.criar(requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/tecnicos/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nome").value("Carlos Souza"))
                    .andExpect(jsonPath("$.email").value("carlos@empresa.com"))
                    .andExpect(jsonPath("$.perfil").value("TECNICO"));
        }

        @Test
        @DisplayName("retorna 400 com detalhes quando nome, e-mail e perfil estão ausentes")
        void retorna400QuandoCamposObrigatoriosVazios() throws Exception {
            String body = json(new TecnicoRequest("", "", null));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                    .andExpect(jsonPath("$.detalhes", hasItems(
                            "nome: Nome é obrigatório",
                            "email: E-mail é obrigatório",
                            "perfil: Perfil é obrigatório")));

            verifyNoInteractions(tecnicoService);
        }

        @Test
        @DisplayName("retorna 400 quando o e-mail é inválido")
        void retorna400QuandoEmailInvalido() throws Exception {
            String body = json(new TecnicoRequest("Carlos", "email-invalido", Perfil.ADMIN));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("email: E-mail inválido"));

            verifyNoInteractions(tecnicoService);
        }

        @Test
        @DisplayName("retorna 400 quando o nome excede o tamanho máximo")
        void retorna400QuandoNomeMuitoLongo() throws Exception {
            String body = json(new TecnicoRequest("a".repeat(101), "carlos@empresa.com", Perfil.ADMIN));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("nome: Nome deve ter no máximo 100 caracteres"));

            verifyNoInteractions(tecnicoService);
        }

        @Test
        @DisplayName("retorna 400 quando o perfil informado não existe no enum")
        void retorna400QuandoPerfilInvalido() throws Exception {
            String body = """
                    {"nome": "Carlos", "email": "carlos@empresa.com", "perfil": "INEXISTENTE"}
                    """;

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(tecnicoService);
        }

        @Test
        @DisplayName("retorna 409 quando o service lança RegraNegocioException")
        void retorna409QuandoEmailDuplicado() throws Exception {
            when(tecnicoService.criar(any(TecnicoRequest.class)))
                    .thenThrow(new RegraNegocioException("Já existe tecnico com o e-mail carlos@empresa.com"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value("Já existe tecnico com o e-mail carlos@empresa.com"));
        }
    }

    @Nested
    @DisplayName("GET /tecnicos")
    class Listar {

        @Test
        @DisplayName("retorna 200 com a lista de técnicos")
        void retorna200ComLista() throws Exception {
            when(tecnicoService.listar()).thenReturn(List.of(
                    responseEsperado,
                    new TecnicoResponse(2L, "Ana", "ana@empresa.com", Perfil.ADMIN)));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].perfil").value("ADMIN"));
        }

        @Test
        @DisplayName("retorna 200 com lista vazia")
        void retorna200ComListaVazia() throws Exception {
            when(tecnicoService.listar()).thenReturn(List.of());

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("GET /tecnicos/{id}")
    class Buscar {

        @Test
        @DisplayName("retorna 200 com o técnico")
        void retorna200() throws Exception {
            when(tecnicoService.buscar(ID)).thenReturn(responseEsperado);

            mockMvc.perform(get(URL + "/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("carlos@empresa.com"));
        }

        @Test
        @DisplayName("retorna 404 quando o técnico não existe")
        void retorna404() throws Exception {
            when(tecnicoService.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Tecnico 99 não encontrado"));

            mockMvc.perform(get(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.erro").value("Tecnico 99 não encontrado"));
        }

        @Test
        @DisplayName("retorna 400 quando o id não é numérico")
        void retorna400QuandoIdInvalido() throws Exception {
            mockMvc.perform(get(URL + "/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(tecnicoService);
        }
    }

    @Nested
    @DisplayName("PUT /tecnicos/{id}")
    class Atualizar {

        @Test
        @DisplayName("retorna 200 com o técnico atualizado")
        void retorna200() throws Exception {
            when(tecnicoService.atualizar(ID, requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Carlos Souza"));
        }

        @Test
        @DisplayName("retorna 400 e não chama o service quando o request é inválido")
        void retorna400QuandoRequestInvalido() throws Exception {
            String body = json(new TecnicoRequest(" ", "carlos@empresa.com", Perfil.TECNICO));

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("nome: Nome é obrigatório"));

            verify(tecnicoService, never()).atualizar(any(), any());
        }

        @Test
        @DisplayName("retorna 404 quando o técnico não existe")
        void retorna404() throws Exception {
            when(tecnicoService.atualizar(eq(99L), any(TecnicoRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Tecnico 99 não encontrado"));

            mockMvc.perform(put(URL + "/{id}", 99L).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("retorna 409 quando o e-mail já pertence a outro técnico")
        void retorna409() throws Exception {
            when(tecnicoService.atualizar(eq(ID), any(TecnicoRequest.class)))
                    .thenThrow(new RegraNegocioException("Já existe um tecnico com o e-mail: carlos@empresa.com"));

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /tecnicos/{id}")
    class Excluir {

        @Test
        @DisplayName("retorna 204 sem corpo e chama o service")
        void retorna204() throws Exception {
            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(tecnicoService).excluir(ID);
        }

        @Test
        @DisplayName("retorna 404 quando o técnico não existe")
        void retorna404() throws Exception {
            doThrow(new RecursoNaoEncontradoException("Tecnico 99 não encontrado"))
                    .when(tecnicoService).excluir(99L);

            mockMvc.perform(delete(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("retorna 409 quando o técnico possui chamados vinculados")
        void retorna409QuandoPossuiVinculo() throws Exception {
            doThrow(new DataIntegrityViolationException("fk"))
                    .when(tecnicoService).excluir(ID);

            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isConflict());
        }
    }
}
