package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Garante que as telas Thymeleaf renderizam (erros de template só aparecem em tempo de execução).
 */
@WebMvcTest(HomeController.class)
@DisplayName("HomeController (telas)")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("GET / renderiza o painel com o layout compartilhado")
    void inicio() throws Exception {
        when(dashboardService.carregar())
                .thenReturn(new DashboardResponse(0, 0, 0, 0, 0, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Painel de chamados")))
                .andExpect(content().string(containsString("id=\"sidebar\"")))
                .andExpect(content().string(containsString("/js/layout.js")));
    }

    @Test
    @DisplayName("GET /telas/categorias renderiza a tela de categorias com o menu marcado")
    void categorias() throws Exception {
        mockMvc.perform(get("/telas/categorias"))
                .andExpect(status().isOk())
                .andExpect(view().name("categorias"))
                .andExpect(content().string(containsString("<h1>Categorias</h1>")))
                .andExpect(content().string(containsString("data-api=\"/categorias\"")))
                .andExpect(content().string(containsString("href=\"/telas/categorias\" aria-current=\"page\"")))
                .andExpect(content().string(containsString("/js/categorias.js")));
    }

    @Test
    @DisplayName("GET /telas/tecnicos renderiza a tela de técnicos com o menu marcado")
    void tecnicos() throws Exception {
        mockMvc.perform(get("/telas/tecnicos"))
                .andExpect(status().isOk())
                .andExpect(view().name("tecnicos"))
                .andExpect(content().string(containsString("<h1>Técnicos</h1>")))
                .andExpect(content().string(containsString("data-api=\"/tecnicos\"")))
                .andExpect(content().string(containsString("href=\"/telas/tecnicos\" aria-current=\"page\"")))
                .andExpect(content().string(containsString("/js/tecnicos.js")));
    }

    @Test
    @DisplayName("GET /telas/clientes renderiza a tela de clientes com o menu marcado")
    void clientes() throws Exception {
        mockMvc.perform(get("/telas/clientes"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes"))
                .andExpect(content().string(containsString("<h1>Clientes</h1>")))
                .andExpect(content().string(containsString("data-api=\"/clientes\"")))
                .andExpect(content().string(containsString("href=\"/telas/clientes\" aria-current=\"page\"")))
                .andExpect(content().string(containsString("/js/clientes.js")));
    }
}
