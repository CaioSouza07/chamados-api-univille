package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;

import java.time.LocalDate;

public record ChamadoResponse(

        Long id,
        String titulo,
        String descricao,
        LocalDate dataAbertura,
        LocalDate dataFechamento,
        Prioridade prioridade,
        StatusChamado status,
        ClienteResponse cliente,
        TecnicoResponse tecnico,
        CategoriaResponse categoria
) {
    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(), chamado.getDataAbertura(), chamado.getDataFechamento(), chamado.getPrioridade(), chamado.getStatus(), ClienteResponse.de(chamado.getCliente()), TecnicoResponse.de(chamado.getTecnico()), CategoriaResponse.de(chamado.getCategoria()));
    }
}
