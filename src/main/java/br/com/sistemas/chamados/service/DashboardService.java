package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.dto.DashboardResponse.AtividadeResumo;
import br.com.sistemas.chamados.dto.DashboardResponse.PrioridadeResumo;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.HistoricoStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class DashboardService {

    private static final List<Prioridade> ORDEM_PRIORIDADES = List.of(Prioridade.CRITICA, Prioridade.ALTA, Prioridade.MEDIA, Prioridade.BAIXA);

    private final ChamadoRepository chamadoRepository;
    private final HistoricoStatusRepository historicoStatusRepository;

    public DashboardService(ChamadoRepository chamadoRepository, HistoricoStatusRepository historicoStatusRepository) {
        this.chamadoRepository = chamadoRepository;
        this.historicoStatusRepository = historicoStatusRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse carregar() {
        LocalDate hoje = LocalDate.now();

        return new DashboardResponse(
                chamadoRepository.countByStatus(StatusChamado.ABERTO),
                chamadoRepository.countByStatusAndDataAbertura(StatusChamado.ABERTO, hoje),
                chamadoRepository.countByStatus(StatusChamado.EM_ATENDIMENTO),
                chamadoRepository.countByStatus(StatusChamado.AGUARDANDO_CLIENTE),
                chamadoRepository.countByDataFechamentoGreaterThanEqual(hoje.withDayOfMonth(1)),
                chamadoRepository.findTop5ByOrderByIdDesc().stream().map(ChamadoResponse::de).toList(),
                emAbertoPorPrioridade(),
                historicoStatusRepository.findTop5ByOrderByDataAlteracaoDesc().stream().map(AtividadeResumo::de).toList()
        );
    }

    private List<PrioridadeResumo> emAbertoPorPrioridade() {
        List<Long> quantidades = ORDEM_PRIORIDADES.stream()
                .map(p -> chamadoRepository.countByPrioridadeAndStatusNot(p, StatusChamado.FECHADO))
                .toList();
        long total = quantidades.stream().mapToLong(Long::longValue).sum();

        return IntStream.range(0, ORDEM_PRIORIDADES.size())
                .mapToObj(i -> PrioridadeResumo.de(ORDEM_PRIORIDADES.get(i), quantidades.get(i), total))
                .toList();
    }
}
