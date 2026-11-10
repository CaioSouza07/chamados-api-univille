package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.service.CategoriaService;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriaController.class)
@DisplayName("CategoriaController")
class CategoriaControllerTest {

    private static final Long ID = 1L;
    private static final String URL = "/categorias";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private CategoriaService categoriaService;

    private final CategoriaRequest requestValido = new CategoriaRequest("Hardware", "Problemas com equipamentos");
    private final CategoriaResponse responseEsperado = new CategoriaResponse(ID, "Hardware", "Problemas com equipamentos");

    private String json(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /categorias")
    class Criar {

        @Test
        @DisplayName("retorna 201 com o corpo quando o request é válido")
        void retorna201() throws Exception {
            when(categoriaService.criar(requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nome").value("Hardware"))
                    .andExpect(jsonPath("$.descricao").value("Problemas com equipamentos"));
        }

        @Test
        @DisplayName("retorna 400 com detalhes quando nome e descrição estão em branco")
        void retorna400QuandoCamposObrigatoriosVazios() throws Exception {
            String body = json(new CategoriaRequest("", ""));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                    .andExpect(jsonPath("$.detalhes", hasItems(
                            "nome: Nome é obrigatório",
                            "descricao: Descrição é obrigatório")));

            verifyNoInteractions(categoriaService);
        }

        @Test
        @DisplayName("retorna 400 quando nome e descrição excedem o tamanho máximo")
        void retorna400QuandoTamanhoExcedido() throws Exception {
            String body = json(new CategoriaRequest("a".repeat(51), "b".repeat(121)));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes", hasItem("nome: Nome deve ter no máximo 50 caracteres")))
                    .andExpect(jsonPath("$.detalhes", hasItem(startsWith("descricao: "))));

            verifyNoInteractions(categoriaService);
        }

        @Test
        @DisplayName("retorna 400 quando o corpo é um JSON malformado")
        void retorna400QuandoJsonMalformado() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoriaService);
        }
    }

    @Nested
    @DisplayName("GET /categorias")
    class Listar {

        @Test
        @DisplayName("retorna 200 com a lista de categorias")
        void retorna200ComLista() throws Exception {
            when(categoriaService.listar()).thenReturn(List.of(
                    responseEsperado,
                    new CategoriaResponse(2L, "Software", "Programas")));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].nome").value("Software"));
        }

        @Test
        @DisplayName("retorna 200 com lista vazia")
        void retorna200ComListaVazia() throws Exception {
            when(categoriaService.listar()).thenReturn(List.of());

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("GET /categorias/{id}")
    class Buscar {

        @Test
        @DisplayName("retorna 200 com a categoria")
        void retorna200() throws Exception {
            when(categoriaService.buscar(ID)).thenReturn(responseEsperado);

            mockMvc.perform(get(URL + "/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nome").value("Hardware"));
        }

        @Test
        @DisplayName("retorna 404 quando a categoria não existe")
        void retorna404() throws Exception {
            when(categoriaService.buscar(99L))
                    .thenThrow(new RecursoNaoEncontradoException("Categoria 99 não encontrado"));

            mockMvc.perform(get(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.erro").value("Categoria 99 não encontrado"));
        }

        @Test
        @DisplayName("retorna 400 quando o id não é numérico")
        void retorna400QuandoIdInvalido() throws Exception {
            mockMvc.perform(get(URL + "/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoriaService);
        }
    }

    @Nested
    @DisplayName("PUT /categorias/{id}")
    class Atualizar {

        @Test
        @DisplayName("retorna 200 com a categoria atualizada")
        void retorna200() throws Exception {
            when(categoriaService.atualizar(ID, requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Hardware"));
        }

        @Test
        @DisplayName("retorna 400 e não chama o service quando o request é inválido")
        void retorna400QuandoRequestInvalido() throws Exception {
            String body = json(new CategoriaRequest(" ", "Descrição"));

            mockMvc.perform(put(URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("nome: Nome é obrigatório"));

            verify(categoriaService, never()).atualizar(any(), any());
        }

        @Test
        @DisplayName("retorna 404 quando a categoria não existe")
        void retorna404() throws Exception {
            when(categoriaService.atualizar(eq(99L), any(CategoriaRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Categoria 99 não encontrado"));

            mockMvc.perform(put(URL + "/{id}", 99L).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /categorias/{id}")
    class Excluir {

        @Test
        @DisplayName("retorna 204 sem corpo e chama o service")
        void retorna204() throws Exception {
            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(categoriaService).excluir(ID);
        }

        @Test
        @DisplayName("retorna 404 quando a categoria não existe")
        void retorna404() throws Exception {
            doThrow(new RecursoNaoEncontradoException("Categoria 99 não encontrado"))
                    .when(categoriaService).excluir(99L);

            mockMvc.perform(delete(URL + "/{id}", 99L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("retorna 409 quando a categoria possui chamados vinculados")
        void retorna409QuandoPossuiVinculo() throws Exception {
            doThrow(new DataIntegrityViolationException("fk"))
                    .when(categoriaService).excluir(ID);

            mockMvc.perform(delete(URL + "/{id}", ID))
                    .andExpect(status().isConflict());
        }
    }
}
