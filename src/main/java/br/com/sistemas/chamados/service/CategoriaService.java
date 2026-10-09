package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest dto) {
        Categoria salvo = categoriaRepository.save(new Categoria(dto.nome(), dto.descricao()));
        return CategoriaResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.de(buscarEntity(id));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest dto) {
        Categoria categoria = buscarEntity(id);
        categoria.setNome(dto.nome());
        categoria.setDescricao(dto.descricao());
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        categoriaRepository.delete(buscarEntity(id));
    }

    private Categoria buscarEntity(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " não encontrado"));
    }
}
