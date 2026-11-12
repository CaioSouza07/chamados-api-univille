package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.enums.TipoAutor;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.service.ComentarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComentarioController.class)
@DisplayName("ComentarioController")
class ComentarioControllerTest {

    private static final Long CHAMADO_ID = 1L;
    private static final String URL = "/chamados/{chamadoId}/comentarios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ComentarioService comentarioService;

    private final ComentarioRequest requestValido = new ComentarioRequest("Já reiniciei o equipamento", TipoAutor.CLIENTE, 10L);
    private final ComentarioResponse responseEsperado = new ComentarioResponse(100L, "Já reiniciei o equipamento",
            LocalDateTime.of(2026, 1, 10, 9, 30), TipoAutor.CLIENTE, 10L, "Maria Silva");

    private String json(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /chamados/{chamadoId}/comentarios")
    class Criar {

        @Test
        @DisplayName("retorna 201 com Location do chamado e corpo quando o request é válido")
        void retorna201() throws Exception {
            when(comentarioService.criar(CHAMADO_ID, requestValido)).thenReturn(responseEsperado);

            mockMvc.perform(post(URL, CHAMADO_ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/chamados/1"))
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.texto").value("Já reiniciei o equipamento"))
                    .andExpect(jsonPath("$.tipoAutor").value("CLIENTE"))
                    .andExpect(jsonPath("$.autorId").value(10))
                    .andExpect(jsonPath("$.autorNome").value("Maria Silva"))
                    .andExpect(jsonPath("$.dataCriacao").value("2026-01-10T09:30:00"));
        }

        @Test
        @DisplayName("retorna 400 com detalhes quando texto, tipoAutor e autorId estão ausentes")
        void retorna400QuandoCamposObrigatoriosAusentes() throws Exception {
            String body = json(new ComentarioRequest("", null, null));

            mockMvc.perform(post(URL, CHAMADO_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                    .andExpect(jsonPath("$.detalhes", hasItems(
                            "texto: Texto é obrigatório",
                            "tipoAutor: Tipo do autor é obrigatório",
                            "autorId: Autor é obrigatório")));

            verifyNoInteractions(comentarioService);
        }

        @Test
        @DisplayName("retorna 400 quando o texto excede 500 caracteres")
        void retorna400QuandoTextoMuitoLongo() throws Exception {
            String body = json(new ComentarioRequest("t".repeat(501), TipoAutor.TECNICO, 30L));

            mockMvc.perform(post(URL, CHAMADO_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes[0]").value("texto: Texto deve ter no máximo 500 caracteres"));

            verifyNoInteractions(comentarioService);
        }

        @Test
        @DisplayName("retorna 400 quando o tipoAutor não existe no enum")
        void retorna400QuandoTipoAutorInvalido() throws Exception {
            String body = """
                    {"texto": "Olá", "tipoAutor": "ADMIN", "autorId": 10}
                    """;

            mockMvc.perform(post(URL, CHAMADO_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(comentarioService);
        }

        @Test
        @DisplayName("retorna 400 quando o chamadoId não é numérico")
        void retorna400QuandoChamadoIdInvalido() throws Exception {
            mockMvc.perform(post(URL, "abc").contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(comentarioService);
        }

        @Test
        @DisplayName("retorna 409 quando o chamado está fechado")
        void retorna409QuandoChamadoFechado() throws Exception {
            when(comentarioService.criar(eq(CHAMADO_ID), any(ComentarioRequest.class)))
                    .thenThrow(new ChamadoFechadoException("Chamado está fechado, não é possível adicionar comentários"));

            mockMvc.perform(post(URL, CHAMADO_ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value("Chamado está fechado, não é possível adicionar comentários"));
        }

        @Test
        @DisplayName("retorna 404 quando o chamado não existe")
        void retorna404QuandoChamadoNaoExiste() throws Exception {
            when(comentarioService.criar(eq(99L), any(ComentarioRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

            mockMvc.perform(post(URL, 99L).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.erro").value("Chamado 99 não encontrado"));
        }

        @Test
        @DisplayName("retorna 404 quando o autor não existe")
        void retorna404QuandoAutorNaoExiste() throws Exception {
            when(comentarioService.criar(eq(CHAMADO_ID), any(ComentarioRequest.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Cliente 10 não encontrado"));

            mockMvc.perform(post(URL, CHAMADO_ID).contentType(MediaType.APPLICATION_JSON).content(json(requestValido)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.erro").value("Cliente 10 não encontrado"));
        }
    }
}
