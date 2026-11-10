package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    List<HistoricoStatus> findTop5ByOrderByDataAlteracaoDesc();
}
