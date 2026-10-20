package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.ChamadoAtribuirRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ChamadoStatusRequest;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final ClienteService clienteService;
    private final TecnicoService tecnicoService;
    private final CategoriaService categoriaService;

    public ChamadoService(ChamadoRepository chamadoRepository, ClienteService clienteService, TecnicoService tecnicoService, CategoriaService categoriaService) {
        this.chamadoRepository = chamadoRepository;
        this.clienteService = clienteService;
        this.tecnicoService = tecnicoService;
        this.categoriaService = categoriaService;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Cliente cliente = clienteService.buscarEntity(dto.clienteId());
        Categoria categoria = categoriaService.buscarEntity(dto.categoriaId());
        Chamado salvo = chamadoRepository.save(new Chamado(dto.titulo(), dto.descricao(), dto.prioridade(), cliente, categoria));
        return ChamadoResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listar(Pageable paginacao, StatusChamado status, Prioridade prioridade, Long clientId) {
        Cliente cliente = clienteService.buscarEntity(clientId);
        return chamadoRepository.findAllOptionalByStatusAndPrioridadeAndClienteId(status, prioridade, cliente, paginacao).map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(Long id) {
        return ChamadoResponse.de(buscarEntity(id));
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);

        if (chamado.getStatus() == StatusChamado.FECHADO){
            throw new ChamadoFechadoException("Chamado está fechado, não é possível realizar qualquer alteração");
        }
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(clienteService.buscarEntity(dto.clienteId()));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse atribuir(Long id, ChamadoAtribuirRequest dto) {
        Chamado chamado = buscarEntity(id);

        if (chamado.getStatus() == StatusChamado.FECHADO){
            throw new ChamadoFechadoException("Chamado está fechado, não é possível realizar qualquer alteração");
        }
        chamado.setTecnico(tecnicoService.buscarEntity(dto.tecnicoId()));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(Long id, ChamadoStatusRequest dto) {
        Chamado chamado = buscarEntity(id);

        if (dto.status() == StatusChamado.FECHADO && chamado.getStatus() != StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDate.now());
        } else if (chamado.getStatus() == StatusChamado.FECHADO && dto.status() != StatusChamado.FECHADO) {
            chamado.setDataFechamento(null);
        }
        chamado.setStatus(dto.status());
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        chamadoRepository.delete(buscarEntity(id));
    }

    public Chamado buscarEntity(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + id + " não encontrado"));
    }
}
