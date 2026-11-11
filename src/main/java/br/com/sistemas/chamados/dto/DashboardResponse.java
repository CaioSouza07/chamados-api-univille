package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dados exibidos na tela inicial (painel de chamados).
 */
public record DashboardResponse(
        long abertos,
        long abertosHoje,
        long emAtendimento,
        long aguardandoCliente,
        long fechadosNoMes,
        List<ChamadoResponse> recentes,
        List<PrioridadeResumo> porPrioridade,
        List<AtividadeResumo> atividades
) {

    public record PrioridadeResumo(Prioridade prioridade, long quantidade, int percentual) {

        public static PrioridadeResumo de(Prioridade prioridade, long quantidade, long total) {
            int percentual = total == 0 ? 0 : (int) Math.round(quantidade * 100.0 / total);
            return new PrioridadeResumo(prioridade, quantidade, percentual);
        }
    }

    public record AtividadeResumo(Long chamadoId, StatusChamado statusAnterior, StatusChamado statusNovo, LocalDateTime data) {

        public static AtividadeResumo de(HistoricoStatus historico) {
            return new AtividadeResumo(historico.getChamado().getId(), historico.getStatusAnterior(), historico.getStatusNovo(), historico.getDataAlteracao());
        }
    }
}
