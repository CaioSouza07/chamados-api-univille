package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.enums.StatusChamado;

import java.time.LocalDateTime;

public record HistoricoStatusResponse(

        Long id,
        StatusChamado statusAnterior,
        StatusChamado statusNovo,
        LocalDateTime dataAlteracao
) {
    public static HistoricoStatusResponse de(HistoricoStatus historico) {
        return new HistoricoStatusResponse(historico.getId(), historico.getStatusAnterior(), historico.getStatusNovo(), historico.getDataAlteracao());
    }
}
