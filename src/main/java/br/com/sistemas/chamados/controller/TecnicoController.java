package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.service.TecnicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @PostMapping
    public ResponseEntity<TecnicoResponse> criar(@Valid @RequestBody TecnicoRequest dto) {
        TecnicoResponse criado = tecnicoService.criar(dto);
        return ResponseEntity.created(URI.create("/tecnicos/" + criado.id())).body(criado);
    }

    @GetMapping
    public List<TecnicoResponse> listar() {
        return tecnicoService.listar();
    }

    @GetMapping("/{id}")
    public TecnicoResponse buscar(@PathVariable Long id) {
        return tecnicoService.buscar(id);
    }

    @PutMapping("/{id}")
    public TecnicoResponse atualizar(@PathVariable Long id, @Valid @RequestBody TecnicoRequest dto) {
        return tecnicoService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        tecnicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
