package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
}
