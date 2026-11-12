/*
 * Tela de detalhe do chamado: dados, status, técnico, comentários, histórico, edição e exclusão.
 * Tudo vem de GET /chamados/{id}; depois de cada ação o chamado é recarregado e a tela redesenhada.
 */
(function () {
  const tela = document.getElementById('tela-chamado');
  const ID = tela.dataset.id;
  const URL_CHAMADO = tela.dataset.api + '/' + ID;  // /chamados/{id}
  const API_TECNICOS = tela.dataset.apiTecnicos;    // /tecnicos
  const TELA_LISTA = tela.dataset.tela;             // /telas/chamados

  const $ = (id) => document.getElementById(id);
  const estado = $('estado');
  const detalhe = $('detalhe');
  const btnEditar = $('btn-editar');
  const btnExcluir = $('btn-excluir');
  const acoesStatus = $('d-acoes-status');
  const campoTecnico = $('d-tecnico');
  const btnAtribuir = $('btn-atribuir');
  const formComentario = $('form-comentario');
  const campoAutor = $('comentario-autor');
  const campoTexto = $('comentario-texto');
  const comentarioErro = $('comentario-erro');
  const btnComentar = $('btn-comentar');
  const atualizarContadorComentario = ui.bindCounters(formComentario);

  const modalExcluir = ui.modal($('modal-excluir'));
  const excluirErro = $('excluir-erro');
  const btnConfirmarExclusao = $('btn-confirmar-exclusao');

  let chamado = null;
  let tecnicos = [];
  let ocupado = false; // evita duas ações ao mesmo tempo (ex.: duplo clique)

  // ---------------- Carregamento ----------------

  async function carregar() {
    try {
      const atualizado = await api.get(URL_CHAMADO);
      await tecnicosProntos; // a lista de técnicos é buscada em paralelo, uma vez só
      chamado = atualizado;
      renderizar();
      estado.hidden = true;
      detalhe.hidden = false;
      document.title = 'Chamado #' + chamado.id + ' · ' + chamado.titulo + ' · Sistema de Chamados';
    } catch (e) {
      mostrarEstadoDeErro(e);
    }
  }

  async function carregarTecnicos() {
    try {
      tecnicos = await api.get(API_TECNICOS);
      tecnicos.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
    } catch (e) {
      tecnicos = []; // sem técnicos a tela continua; só atribuição/comentar como técnico ficam vazios
    }
  }

  function mostrarEstadoDeErro(erro) {
    detalhe.hidden = true;
    estado.hidden = false;
    const p = document.createElement('div');
    p.className = 'empty' + (erro.status === 404 ? '' : ' empty-error');
    p.textContent = erro.status === 404
      ? 'Chamado #' + ID + ' não encontrado. Ele pode ter sido excluído.'
      : erro.message;

    const acao = document.createElement('div');
    acao.className = 'empty-action';
    if (erro.status === 404) {
      const voltar = document.createElement('a');
      voltar.className = 'btn btn-ghost btn-sm';
      voltar.href = TELA_LISTA;
      voltar.append(ui.icon('i-arrow-left'), 'Voltar para a lista');
      acao.appendChild(voltar);
    } else {
      const retry = document.createElement('button');
      retry.type = 'button';
      retry.className = 'btn btn-ghost btn-sm';
      retry.append(ui.icon('i-refresh'), 'Tentar novamente');
      retry.addEventListener('click', carregar);
      acao.appendChild(retry);
    }
    p.appendChild(acao);
    estado.replaceChildren(p);
  }

  // ---------------- Renderização ----------------

  const fechado = () => chamado.status === 'FECHADO';

  function renderizar() {
    const status = dominio.status(chamado.status);
    const prioridade = dominio.prioridade(chamado.prioridade);

    $('d-numero').textContent = '#' + chamado.id;
    $('d-abertura').textContent = dominio.data(chamado.dataAbertura);
    $('d-titulo').textContent = chamado.titulo;
    $('d-descricao').textContent = chamado.descricao;

    const badge = document.createElement('span');
    badge.className = 'badge badge-' + status.css;
    badge.textContent = status.label;
    const prio = document.createElement('span');
    prio.className = 'prio-tag prio-tag-' + prioridade.css;
    prio.textContent = 'Prioridade ' + prioridade.label.toLowerCase();
    $('d-badges').replaceChildren(badge, prio);

    $('aviso-fechado').hidden = !fechado();
    $('aviso-fechado-titulo').textContent = 'Chamado fechado em ' + dominio.data(chamado.dataFechamento) + '.';
    btnEditar.disabled = fechado();
    btnEditar.title = fechado() ? 'Reabra o chamado para editar' : '';

    renderizarAcoesDeStatus();
    renderizarTecnico();
    renderizarDados();
    renderizarComentarios();
    renderizarHistorico();
  }

  function renderizarAcoesDeStatus() {
    acoesStatus.replaceChildren(...dominio.acoesDeStatus(chamado.status).map((acao) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'btn btn-sm ' + (acao.principal ? 'btn-primary' : 'btn-ghost');
      b.textContent = acao.label;
      b.addEventListener('click', () => mudarStatus(acao.status));
      return b;
    }));
  }

  function renderizarTecnico() {
    const atual = chamado.tecnico ? String(chamado.tecnico.id) : '';
    const opcoes = tecnicos.map((t) => new Option(t.nome, t.id));
    const vazio = new Option(tecnicos.length ? 'Selecione um técnico' : 'Nenhum técnico cadastrado', '');
    vazio.disabled = Boolean(atual); // depois de atribuído, a API não permite "desatribuir"
    campoTecnico.replaceChildren(vazio, ...opcoes);
    campoTecnico.value = atual;
    campoTecnico.disabled = fechado() || !tecnicos.length;
    atualizarBotaoAtribuir();
  }

  function atualizarBotaoAtribuir() {
    const atual = chamado.tecnico ? String(chamado.tecnico.id) : '';
    btnAtribuir.disabled = fechado() || !campoTecnico.value || campoTecnico.value === atual;
    btnAtribuir.lastChild.textContent = atual ? ' Trocar' : ' Atribuir';
  }

  function item(dl, rotulo, ...conteudo) {
    const dt = document.createElement('dt');
    dt.textContent = rotulo;
    const dd = document.createElement('dd');
    dd.append(...conteudo);
    dl.append(dt, dd);
  }

  function linhaMeta(texto) {
    const span = document.createElement('span');
    span.className = 't-meta';
    span.textContent = texto;
    return span;
  }

  function renderizarDados() {
    const dl = $('d-dados');
    dl.replaceChildren();
    const cliente = chamado.cliente;

    const nomeCliente = document.createElement('strong');
    nomeCliente.textContent = cliente.nome;
    const verChamados = document.createElement('a');
    verChamados.className = 'link';
    verChamados.href = TELA_LISTA + '?clienteId=' + cliente.id;
    verChamados.textContent = 'Ver chamados do cliente';
    item(dl, 'Cliente', nomeCliente, linhaMeta(cliente.email),
      ...(cliente.telefone ? [linhaMeta(cliente.telefone)] : []), verChamados);

    item(dl, 'Categoria', chamado.categoria.nome);

    if (chamado.tecnico) {
      item(dl, 'Técnico', chamado.tecnico.nome);
    } else {
      const vazio = document.createElement('span');
      vazio.className = 't-vazio';
      vazio.textContent = 'Não atribuído';
      item(dl, 'Técnico', vazio);
    }

    item(dl, 'Aberto em', dominio.data(chamado.dataAbertura));
    if (chamado.dataFechamento) item(dl, 'Fechado em', dominio.data(chamado.dataFechamento));
  }

  function iniciais(nome) {
    const partes = nome.trim().split(/\s+/);
    return ((partes[0] || '')[0] + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
  }

  function renderizarComentarios() {
    const lista = $('d-comentarios');
    const comentarios = chamado.comentarios || [];
    $('d-total-comentarios').textContent = comentarios.length
      ? comentarios.length + (comentarios.length === 1 ? ' comentário' : ' comentários') : '';

    if (!comentarios.length) {
      const li = document.createElement('li');
      li.className = 'empty';
      li.textContent = 'Nenhum comentário ainda.';
      lista.replaceChildren(li);
    } else {
      lista.replaceChildren(...comentarios.map((c) => {
        const deTecnico = c.tipoAutor === 'TECNICO';
        const li = document.createElement('li');
        li.className = 'comment ' + (deTecnico ? 'comment-tecnico' : 'comment-cliente');

        const avatar = document.createElement('span');
        avatar.className = 'comment-avatar';
        avatar.setAttribute('aria-hidden', 'true');
        avatar.textContent = iniciais(c.autorNome);

        const corpo = document.createElement('div');
        corpo.className = 'comment-body';
        const cabecalho = document.createElement('div');
        cabecalho.className = 'comment-head';
        const autor = document.createElement('strong');
        autor.textContent = c.autorNome;
        const papel = document.createElement('span');
        papel.className = 'comment-role';
        papel.textContent = deTecnico ? 'Técnico' : 'Cliente';
        const quando = document.createElement('time');
        quando.dateTime = c.dataCriacao;
        quando.textContent = dominio.dataHora(c.dataCriacao);
        cabecalho.append(autor, papel, quando);
        const texto = document.createElement('p');
        texto.className = 'comment-text';
        texto.textContent = c.texto;
        corpo.append(cabecalho, texto);

        li.append(avatar, corpo);
        return li;
      }));
    }

    formComentario.hidden = fechado();
    $('comentarios-fechados').hidden = !fechado();
    renderizarAutores();
  }

  // "Comentar como": o cliente dono do chamado ou um técnico (a API só aceita o cliente do próprio chamado).
  function renderizarAutores() {
    const escolhaAnterior = campoAutor.value;
    const grupoCliente = document.createElement('optgroup');
    grupoCliente.label = 'Cliente';
    grupoCliente.appendChild(new Option(chamado.cliente.nome, 'CLIENTE:' + chamado.cliente.id));
    const grupos = [grupoCliente];
    if (tecnicos.length) {
      const grupoTecnicos = document.createElement('optgroup');
      grupoTecnicos.label = 'Técnicos';
      tecnicos.forEach((t) => grupoTecnicos.appendChild(new Option(t.nome, 'TECNICO:' + t.id)));
      grupos.push(grupoTecnicos);
    }
    campoAutor.replaceChildren(...grupos);

    const padrao = chamado.tecnico ? 'TECNICO:' + chamado.tecnico.id : 'CLIENTE:' + chamado.cliente.id;
    campoAutor.value = escolhaAnterior;
    if (!campoAutor.value) campoAutor.value = padrao;
  }

  function renderizarHistorico() {
    const lista = $('d-historico');
    // Mais recente primeiro, como em "Atividade recente" do painel.
    const eventos = [...(chamado.historicoStatus || [])].reverse();
    if (!eventos.length) {
      const li = document.createElement('li');
      li.className = 'empty';
      li.textContent = 'Sem alterações registradas.';
      lista.replaceChildren(li);
      return;
    }
    lista.replaceChildren(...eventos.map((h) => {
      const li = document.createElement('li');
      const ponto = document.createElement('span');
      ponto.className = 'activity-dot';
      const box = document.createElement('div');
      const p = document.createElement('p');
      const novo = document.createElement('strong');
      novo.textContent = dominio.status(h.statusNovo).label;
      if (h.statusAnterior == null) {
        p.append('Chamado aberto como ', novo);
      } else {
        const anterior = document.createElement('strong');
        anterior.textContent = dominio.status(h.statusAnterior).label;
        p.append('De ', anterior, ' para ', novo);
      }
      const quando = document.createElement('time');
      quando.dateTime = h.dataAlteracao;
      quando.textContent = dominio.dataHora(h.dataAlteracao);
      box.append(p, quando);
      li.append(ponto, box);
      return li;
    }));
  }

  // ---------------- Ações ----------------

  async function executar(acao, mensagemSucesso) {
    if (ocupado) return;
    ocupado = true;
    tela.classList.add('is-busy');
    try {
      await acao();
      if (mensagemSucesso) ui.toast(mensagemSucesso);
    } catch (e) {
      ui.toast(e.message, 'error');
    } finally {
      await carregar(); // mesmo com erro: a tela volta a refletir o estado real do servidor
      ocupado = false;
      tela.classList.remove('is-busy');
    }
  }

  function mudarStatus(novoStatus) {
    executar(
      () => api.patch(URL_CHAMADO + '/status', { status: novoStatus }),
      'Status alterado para “' + dominio.status(novoStatus).label + '”.'
    );
  }

  campoTecnico.addEventListener('change', atualizarBotaoAtribuir);
  btnAtribuir.addEventListener('click', () => {
    const tecnico = tecnicos.find((t) => String(t.id) === campoTecnico.value);
    if (!tecnico) return;
    executar(
      () => api.patch(URL_CHAMADO + '/atribuir', { tecnicoId: tecnico.id }),
      'Chamado atribuído a ' + tecnico.nome + '.'
    );
  });

  formComentario.addEventListener('submit', async (e) => {
    e.preventDefault();
    const texto = campoTexto.value.trim();
    campoTexto.setCustomValidity(texto ? '' : 'Escreva o comentário.');
    if (!formComentario.reportValidity() || ocupado) return;

    const [tipoAutor, autorId] = campoAutor.value.split(':');
    ui.hideError(comentarioErro);
    btnComentar.disabled = true;
    ocupado = true;
    try {
      await api.post(URL_CHAMADO + '/comentarios', { texto, tipoAutor, autorId: Number(autorId) });
      campoTexto.value = '';
      atualizarContadorComentario();
      ui.toast('Comentário enviado.');
      await carregar();
    } catch (err) {
      ui.showError(comentarioErro, err);
      if (err.status === 409 || err.status === 404) await carregar(); // ex.: chamado foi fechado em outra aba
    } finally {
      btnComentar.disabled = false;
      ocupado = false;
    }
  });

  campoTexto.addEventListener('input', () => campoTexto.setCustomValidity(''));
  campoTexto.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      formComentario.requestSubmit();
    }
  });

  btnEditar.addEventListener('click', () => {
    chamadoForm.abrir({ chamado, onSalvo: carregar });
  });

  btnExcluir.addEventListener('click', () => {
    $('excluir-nome').textContent = '#' + chamado.id + ' – ' + chamado.titulo;
    ui.hideError(excluirErro);
    btnConfirmarExclusao.disabled = false;
    modalExcluir.open();
    btnConfirmarExclusao.focus();
  });

  btnConfirmarExclusao.addEventListener('click', async () => {
    btnConfirmarExclusao.disabled = true;
    try {
      await api.delete(URL_CHAMADO);
      ui.toastAfterNavigation('Chamado #' + chamado.id + ' excluído.');
      location.href = TELA_LISTA;
    } catch (err) {
      ui.showError(excluirErro, err);
      btnConfirmarExclusao.disabled = false;
    }
  });

  // ---------------- Início ----------------

  const tecnicosProntos = carregarTecnicos();
  carregar();
})();
