/*
 * Rótulos e classes CSS dos enums da API.
 * A API devolve só o nome do enum (ex.: "EM_ATENDIMENTO"); os textos abaixo espelham
 * os enums Java (StatusChamado, Prioridade, Perfil) — mantenha os dois em sincronia.
 *
 *   dominio.status('ABERTO')      -> { label: 'Aberto', css: 'aberto' }      (badge badge-<css>)
 *   dominio.prioridade('ALTA')    -> { label: 'Alta', css: 'alta' }          (prio prio-<css>)
 *   dominio.perfil('ADMIN')       -> { label: 'Administrador', css: 'admin' } (badge badge-perfil-<css>)
 *   dominio.data('2026-11-12')    -> '12/11/2026'
 */
(function () {
  const STATUS = {
    ABERTO: { label: 'Aberto', css: 'aberto' },
    EM_ATENDIMENTO: { label: 'Em atendimento', css: 'atendimento' },
    AGUARDANDO_CLIENTE: { label: 'Aguardando cliente', css: 'aguardando' },
    FECHADO: { label: 'Fechado', css: 'fechado' },
  };

  const PRIORIDADE = {
    BAIXA: { label: 'Baixa', css: 'baixa' },
    MEDIA: { label: 'Média', css: 'media' },
    ALTA: { label: 'Alta', css: 'alta' },
    CRITICA: { label: 'Crítica', css: 'critica' },
  };

  const PERFIL = {
    TECNICO: { label: 'Técnico', css: 'tecnico' },
    ADMIN: { label: 'Administrador', css: 'admin' },
  };

  // Valor desconhecido (enum novo no back): mostra o código bruto em vez de quebrar a tela.
  const buscar = (mapa, padrao) => (valor) => mapa[valor] || { label: String(valor), css: padrao };

  function data(iso) {
    if (!iso) return '';
    const [ano, mes, dia] = String(iso).slice(0, 10).split('-');
    return dia + '/' + mes + '/' + ano;
  }

  window.dominio = {
    STATUS,
    PRIORIDADE,
    PERFIL,
    status: buscar(STATUS, 'aberto'),
    prioridade: buscar(PRIORIDADE, 'baixa'),
    perfil: buscar(PERFIL, 'tecnico'),
    data,
  };
})();
