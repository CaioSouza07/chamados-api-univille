package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.ChamadoAtribuirRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ChamadoStatusRequest;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.ParametroInvalidoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.HistoricoStatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ChamadoService {

    /** Campos aceitos em ?sort= na listagem. "prioridade" é ordenada por gravidade (ver ChamadoRepository). */
    static final List<String> CAMPOS_ORDENAVEIS = List.of("id", "titulo", "dataAbertura", "dataFechamento", "prioridade", "status");

    private final ChamadoRepository chamadoRepository;
    private final ClienteService clienteService;
    private final TecnicoService tecnicoService;
    private final CategoriaService categoriaService;
    private final HistoricoStatusRepository historicoStatusRepository;

    public ChamadoService(ChamadoRepository chamadoRepository, ClienteService clienteService, TecnicoService tecnicoService, CategoriaService categoriaService, HistoricoStatusRepository historicoStatusRepository) {
        this.chamadoRepository = chamadoRepository;
        this.clienteService = clienteService;
        this.tecnicoService = tecnicoService;
        this.categoriaService = categoriaService;
        this.historicoStatusRepository = historicoStatusRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Cliente cliente = clienteService.buscarEntity(dto.clienteId());
        Categoria categoria = categoriaService.buscarEntity(dto.categoriaId());
        Chamado salvo = chamadoRepository.save(new Chamado(dto.titulo(), dto.descricao(), dto.prioridade(), cliente, categoria));
        registrarHistorico(salvo, null, salvo.getStatus());
        return ChamadoResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listar(Pageable paginacao, StatusChamado status, Prioridade prioridade, Long clienteId, String busca) {
        validarOrdenacao(paginacao);
        if (clienteId != null) {
            clienteService.buscarEntity(clienteId);
        }
        return chamadoRepository.pesquisar(status, prioridade, clienteId, busca, paginacao).map(ChamadoResponse::de);
    }

    // Sem isso, um ?sort=campoInexistente estoura como erro 500 na consulta.
    private static void validarOrdenacao(Pageable paginacao) {
        for (Sort.Order order : paginacao.getSort()) {
            if (!CAMPOS_ORDENAVEIS.contains(order.getProperty())) {
                throw new ParametroInvalidoException("Não é possível ordenar por '" + order.getProperty()
                        + "'. Campos aceitos: " + String.join(", ", CAMPOS_ORDENAVEIS));
            }
        }
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscar(Long id) {
        return ChamadoDetalheResponse.de(buscarEntity(id));
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
        chamado.setCategoria(categoriaService.buscarEntity(dto.categoriaId()));
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
        StatusChamado statusAnterior = chamado.getStatus();

        if (dto.status() == StatusChamado.FECHADO && statusAnterior != StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDate.now());
        } else if (statusAnterior == StatusChamado.FECHADO && dto.status() != StatusChamado.FECHADO) {
            chamado.setDataFechamento(null);
        }
        chamado.setStatus(dto.status());

        if (statusAnterior != dto.status()) {
            registrarHistorico(chamado, statusAnterior, dto.status());
        }
        return ChamadoResponse.de(chamado);
    }

    private void registrarHistorico(Chamado chamado, StatusChamado statusAnterior, StatusChamado statusNovo) {
        HistoricoStatus historico = historicoStatusRepository.save(new HistoricoStatus(statusAnterior, statusNovo, chamado));
        chamado.adicionarHistoricoStatus(historico);
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
