/*
 * Rótulos e classes CSS dos enums da API.
 * A API devolve só o nome do enum (ex.: "EM_ATENDIMENTO"); os textos abaixo espelham
 * os enums Java (StatusChamado, Prioridade, Perfil) — mantenha os dois em sincronia.
 *
 *   dominio.status('ABERTO')      -> { label: 'Aberto', css: 'aberto' }      (badge badge-<css>)
 *   dominio.prioridade('ALTA')    -> { label: 'Alta', css: 'alta' }          (prio prio-<css>)
 *   dominio.perfil('ADMIN')       -> { label: 'Administrador', css: 'admin' } (badge badge-perfil-<css>)
 *   dominio.data('2026-11-12')    -> '12/11/2026'
 *   dominio.dataHora('2026-11-12T18:48:20') -> '12/11/2026 às 18:48'
 *   dominio.acoesDeStatus('ABERTO') -> botões de mudança de status oferecidos na tela de detalhe
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

  function dataHora(iso) {
    if (!iso) return '';
    const hora = String(iso).slice(11, 16);
    return data(iso) + (hora ? ' às ' + hora : '');
  }

  // Próximos passos oferecidos para cada status (a API aceita qualquer troca; isto é só a interface).
  // "principal" = ação mais provável, exibida em destaque.
  const ACOES_STATUS = {
    ABERTO: [
      { status: 'EM_ATENDIMENTO', label: 'Iniciar atendimento', principal: true },
      { status: 'AGUARDANDO_CLIENTE', label: 'Aguardar cliente' },
      { status: 'FECHADO', label: 'Fechar chamado' },
    ],
    EM_ATENDIMENTO: [
      { status: 'FECHADO', label: 'Fechar chamado', principal: true },
      { status: 'AGUARDANDO_CLIENTE', label: 'Aguardar cliente' },
      { status: 'ABERTO', label: 'Voltar para aberto' },
    ],
    AGUARDANDO_CLIENTE: [
      { status: 'EM_ATENDIMENTO', label: 'Retomar atendimento', principal: true },
      { status: 'FECHADO', label: 'Fechar chamado' },
    ],
    FECHADO: [
      { status: 'ABERTO', label: 'Reabrir chamado', principal: true },
    ],
  };

  window.dominio = {
    STATUS,
    PRIORIDADE,
    PERFIL,
    status: buscar(STATUS, 'aberto'),
    prioridade: buscar(PRIORIDADE, 'baixa'),
    perfil: buscar(PERFIL, 'tecnico'),
    data,
    dataHora,
    acoesDeStatus: (status) => ACOES_STATUS[status] || [],
  };
})();
