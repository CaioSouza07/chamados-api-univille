package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("painel", dashboardService.carregar());
        return "index";
    }

    /**
     * Telas ficam em /telas/** porque os caminhos "puros" (ex.: /categorias) pertencem à API REST.
     * Os dados são carregados no navegador via fetch na própria API.
     */
    @GetMapping("/telas/categorias")
    public String categorias() {
        return "categorias";
    }

    @GetMapping("/telas/tecnicos")
    public String tecnicos() {
        return "tecnicos";
    }

    @GetMapping("/telas/clientes")
    public String clientes() {
        return "clientes";
    }

    @GetMapping("/telas/chamados")
    public String chamados() {
        return "chamados";
    }

    // A página só recebe o id; os dados do chamado são carregados pelo navegador em /chamados/{id}.
    @GetMapping("/telas/chamados/{id}")
    public String chamado(@PathVariable Long id, Model model) {
        model.addAttribute("chamadoId", id);
        return "chamado";
    }
}
