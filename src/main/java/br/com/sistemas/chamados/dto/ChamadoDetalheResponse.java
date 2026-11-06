package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;

import java.time.LocalDate;
import java.util.List;

public record ChamadoDetalheResponse(

        Long id,
        String titulo,
        String descricao,
        LocalDate dataAbertura,
        LocalDate dataFechamento,
        Prioridade prioridade,
        StatusChamado status,
        ClienteResponse cliente,
        TecnicoResponse tecnico,
        CategoriaResponse categoria,
        List<ComentarioResponse> comentarios,
        List<HistoricoStatusResponse> historicoStatus
) {
    public static ChamadoDetalheResponse de(Chamado chamado) {
        TecnicoResponse tecnico = chamado.getTecnico() != null ? TecnicoResponse.de(chamado.getTecnico()) : null;
        List<ComentarioResponse> comentarios = chamado.getComentarios().stream().map(ComentarioResponse::de).toList();
        List<HistoricoStatusResponse> historicoStatus = chamado.getHistoricoStatus().stream().map(HistoricoStatusResponse::de).toList();

        return new ChamadoDetalheResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(), chamado.getDataAbertura(), chamado.getDataFechamento(), chamado.getPrioridade(), chamado.getStatus(), ClienteResponse.de(chamado.getCliente()), tecnico, CategoriaResponse.de(chamado.getCategoria()), comentarios, historicoStatus);
    }
}
