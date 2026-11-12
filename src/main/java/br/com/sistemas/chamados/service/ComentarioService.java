package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.enums.TipoAutor;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final ChamadoService chamadoService;
    private final ClienteService clienteService;
    private final TecnicoService tecnicoService;

    public ComentarioService(ComentarioRepository comentarioRepository, ChamadoService chamadoService, ClienteService clienteService, TecnicoService tecnicoService) {
        this.comentarioRepository = comentarioRepository;
        this.chamadoService = chamadoService;
        this.clienteService = clienteService;
        this.tecnicoService = tecnicoService;
    }

    @Transactional
    public ComentarioResponse criar(Long chamadoId, ComentarioRequest dto) {
        Chamado chamado = chamadoService.buscarEntity(chamadoId);

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new ChamadoFechadoException("Chamado está fechado, não é possível adicionar comentários");
        }

        Comentario comentario = dto.tipoAutor() == TipoAutor.CLIENTE
                ? new Comentario(dto.texto(), chamado, clienteDoChamado(chamado, dto.autorId()))
                : new Comentario(dto.texto(), chamado, tecnicoService.buscarEntity(dto.autorId()));

        Comentario salvo = comentarioRepository.save(comentario);
        chamado.adicionarComentario(salvo);
        return ComentarioResponse.de(salvo);
    }

    // Um cliente só pode comentar nos próprios chamados.
    private Cliente clienteDoChamado(Chamado chamado, Long autorId) {
        Cliente autor = clienteService.buscarEntity(autorId);
        if (!autor.getId().equals(chamado.getCliente().getId())) {
            throw new RegraNegocioException("Somente o cliente que abriu o chamado pode comentar nele como cliente");
        }
        return autor;
    }
}
