/*
 * Tela de chamados (lista): filtros, busca, ordenação e paginação feitos na API (/chamados).
 * O estado dos filtros fica na URL (?status=ABERTO&pagina=2...), então voltar/avançar do navegador
 * e links vindos de outras telas (ex.: ?clienteId=3, ?busca=..., ?novo=1) funcionam.
 */
(function () {
  const tela = document.getElementById('tela-chamados');
  const API = tela.dataset.api;                 // /chamados
  const API_CLIENTES = tela.dataset.apiClientes; // /clientes
  const TELA = tela.dataset.tela;               // /telas/chamados
  const COLUNAS = 6;
  const TAMANHO_PAGINA = 20;
  const ORDENS = {
    recentes: ['dataAbertura,desc', 'id,desc'],
    antigos: ['dataAbertura,asc', 'id,asc'],
    prioridade: ['prioridade,desc', 'dataAbertura,desc', 'id,desc'],
  };

  const lista = document.getElementById('lista');
  const tabela = lista.closest('.table-wrap');
  const paginacao = document.getElementById('paginacao');
  const abas = [...document.querySelectorAll('#abas-status .tab')];
  const campoBusca = document.getElementById('filtro-busca');
  const campoPrioridade = document.getElementById('filtro-prioridade');
  const campoCliente = document.getElementById('filtro-cliente');
  const campoOrdem = document.getElementById('filtro-ordem');
  const btnLimpar = document.getElementById('btn-limpar');

  const filtros = lerFiltrosDaUrl();
  const pediuNovo = new URLSearchParams(location.search).has('novo'); // lido antes de a URL ser regravada
  let pedidoAtual = 0; // ignora respostas antigas quando o usuário muda o filtro rápido

  // ---------------- Estado <-> URL ----------------

  function lerFiltrosDaUrl() {
    const p = new URLSearchParams(location.search);
    const ordem = p.get('ordem');
    return {
      status: p.get('status') || '',
      prioridade: p.get('prioridade') || '',
      clienteId: p.get('clienteId') || '',
      busca: (p.get('busca') || '').trim(),
      ordem: ORDENS[ordem] ? ordem : 'recentes',
      pagina: Math.max(0, (parseInt(p.get('pagina'), 10) || 1) - 1),
    };
  }

  function gravarFiltrosNaUrl() {
    const p = new URLSearchParams();
    if (filtros.status) p.set('status', filtros.status);
    if (filtros.prioridade) p.set('prioridade', filtros.prioridade);
    if (filtros.clienteId) p.set('clienteId', filtros.clienteId);
    if (filtros.busca) p.set('busca', filtros.busca);
    if (filtros.ordem !== 'recentes') p.set('ordem', filtros.ordem);
    if (filtros.pagina > 0) p.set('pagina', String(filtros.pagina + 1));
    const query = p.toString();
    history.replaceState(null, '', location.pathname + (query ? '?' + query : ''));
  }

  function temFiltro() {
    return Boolean(filtros.status || filtros.prioridade || filtros.clienteId || filtros.busca);
  }

  function sincronizarControles() {
    abas.forEach((aba) => aba.setAttribute('aria-pressed', String(aba.dataset.status === filtros.status)));
    campoBusca.value = filtros.busca;
    campoPrioridade.value = filtros.prioridade;
    campoOrdem.value = filtros.ordem;
    if (campoCliente.querySelector('option[value="' + CSS.escape(filtros.clienteId) + '"]')) {
      campoCliente.value = filtros.clienteId;
    }
    btnLimpar.hidden = !temFiltro();
  }

  // ---------------- Listagem ----------------

  async function carregar() {
    const pedido = ++pedidoAtual;
    gravarFiltrosNaUrl();
    sincronizarControles();
    tabela.classList.add('is-loading');

    const p = new URLSearchParams({ page: filtros.pagina, size: TAMANHO_PAGINA });
    ORDENS[filtros.ordem].forEach((s) => p.append('sort', s));
    if (filtros.status) p.set('status', filtros.status);
    if (filtros.prioridade) p.set('prioridade', filtros.prioridade);
    if (filtros.clienteId) p.set('clienteId', filtros.clienteId);
    if (filtros.busca) p.set('busca', filtros.busca);

    try {
      const resposta = await api.get(API + '?' + p);
      if (pedido !== pedidoAtual) return;
      const info = ui.pageInfo(resposta);

      // Página além do fim (ex.: link antigo ou filtros mudaram): vai para a última existente.
      if (resposta.content.length === 0 && info.number > 0 && info.totalPages > 0) {
        filtros.pagina = info.totalPages - 1;
        return carregar();
      }

      renderizar(resposta.content);
      ui.pagination(paginacao, info, (destino) => {
        filtros.pagina = destino;
        carregar();
        tela.querySelector('.card').scrollIntoView({ block: 'start', behavior: 'smooth' });
      });
    } catch (e) {
      if (pedido !== pedidoAtual) return;
      paginacao.hidden = true;
      ui.tableMessage(lista, COLUNAS, e.message, { error: true, onRetry: carregar });
    } finally {
      if (pedido === pedidoAtual) tabela.classList.remove('is-loading');
    }
  }

  function renderizar(chamados) {
    if (chamados.length === 0) {
      ui.tableMessage(lista, COLUNAS, temFiltro()
        ? 'Nenhum chamado encontrado com os filtros atuais.'
        : 'Nenhum chamado aberto ainda. Clique em “Novo chamado” para registrar o primeiro.');
      return;
    }
    lista.replaceChildren(...chamados.map(linha));
  }

  function celula(classe, conteudo) {
    const td = document.createElement('td');
    if (classe) td.className = classe;
    if (conteudo instanceof Node) td.appendChild(conteudo); else td.textContent = conteudo;
    return td;
  }

  function linha(chamado) {
    const href = TELA + '/' + chamado.id;
    const tr = document.createElement('tr');
    tr.className = 'row-link';
    tr.dataset.href = href;

    const tdChamado = document.createElement('td');
    const titulo = document.createElement('a');
    titulo.className = 't-title t-link';
    titulo.href = href;
    titulo.textContent = chamado.titulo;
    const meta = document.createElement('div');
    meta.className = 't-meta';
    meta.textContent = chamado.cliente.nome + ' · ' + chamado.categoria.nome;

    const prioridade = dominio.prioridade(chamado.prioridade);
    const prio = document.createElement('span');
    prio.className = 'prio prio-' + prioridade.css;
    prio.textContent = prioridade.label;
    // No celular a coluna "Prioridade" some; ela aparece aqui, abaixo do título.
    const prioCelular = document.createElement('div');
    prioCelular.className = 't-meta show-sm';
    prioCelular.appendChild(prio.cloneNode(true));
    tdChamado.append(titulo, meta, prioCelular);

    const status = dominio.status(chamado.status);
    const badge = document.createElement('span');
    badge.className = 'badge badge-' + status.css;
    badge.textContent = status.label;

    tr.append(
      celula('t-id', '#' + chamado.id),
      tdChamado,
      celula('hide-sm', prio),
      celula('', badge),
      celula('hide-sm' + (chamado.tecnico ? '' : ' t-vazio'), chamado.tecnico ? chamado.tecnico.nome : 'Não atribuído'),
      celula('hide-sm t-date', dominio.data(chamado.dataAbertura))
    );
    return tr;
  }

  // ---------------- Filtros ----------------

  function aplicar(mudancas) {
    Object.assign(filtros, mudancas, { pagina: 0 });
    carregar();
  }

  abas.forEach((aba) => aba.addEventListener('click', () => aplicar({ status: aba.dataset.status })));
  campoPrioridade.addEventListener('change', () => aplicar({ prioridade: campoPrioridade.value }));
  campoCliente.addEventListener('change', () => aplicar({ clienteId: campoCliente.value }));
  campoOrdem.addEventListener('change', () => aplicar({ ordem: campoOrdem.value }));

  let esperaBusca = null;
  campoBusca.addEventListener('input', () => {
    clearTimeout(esperaBusca);
    esperaBusca = setTimeout(() => aplicar({ busca: campoBusca.value.trim() }), 300);
  });
  campoBusca.addEventListener('keydown', (e) => {
    if (e.key !== 'Enter') return;
    e.preventDefault();
    clearTimeout(esperaBusca);
    abrirSeForNumero(campoBusca.value).then((abriu) => {
      if (!abriu) aplicar({ busca: campoBusca.value.trim() });
    });
  });

  btnLimpar.addEventListener('click', () => {
    aplicar({ status: '', prioridade: '', clienteId: '', busca: '' });
    campoCliente.value = '';
  });

  async function carregarClientes() {
    try {
      const clientes = await api.get(API_CLIENTES);
      clientes.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
      campoCliente.replaceChildren(
        new Option('Todos os clientes', ''),
        ...clientes.map((c) => new Option(c.nome, c.id))
      );
      sincronizarControles();
    } catch (e) {
      // Sem a lista de clientes o filtro só fica indisponível; o resto da tela continua funcionando.
      campoCliente.disabled = true;
    }
  }

  // "12" ou "#12" na busca: se o chamado existe, abre direto.
  async function abrirSeForNumero(texto) {
    const numero = String(texto).trim().match(/^#?(\d+)$/);
    if (!numero) return false;
    try {
      await api.get(API + '/' + numero[1]);
      location.href = TELA + '/' + numero[1];
      return true;
    } catch (e) {
      return false;
    }
  }

  // ---------------- Novo chamado ----------------

  function abrirNovo() {
    chamadoForm.abrir({
      onSalvo: () => {
        // Volta para a primeira página em "mais recentes" para o chamado novo aparecer no topo.
        Object.assign(filtros, { pagina: 0, ordem: 'recentes' });
        carregar();
      },
    });
  }

  document.getElementById('btn-novo').addEventListener('click', abrirNovo);
  // O "Novo chamado" do topo leva para esta tela com ?novo=1; já estando aqui, só abre o modal.
  const novoTopo = document.getElementById('topbar-novo-chamado');
  if (novoTopo) {
    novoTopo.addEventListener('click', (e) => {
      e.preventDefault();
      abrirNovo();
    });
  }

  // ---------------- Início ----------------

  (async function iniciar() {
    sincronizarControles();
    carregarClientes();

    // Busca vinda do topo com um número: abre o chamado em vez de listar.
    if (filtros.busca && await abrirSeForNumero(filtros.busca)) return;

    carregar();

    if (pediuNovo) {
      abrirNovo();
    }
  })();
})();
