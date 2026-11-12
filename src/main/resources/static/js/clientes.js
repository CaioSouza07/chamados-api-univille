/* Tela de clientes: listagem, filtro, criação, edição, exclusão e chamados do cliente via API REST (/clientes). */
(function () {
  const tela = document.getElementById('tela-clientes');
  const API = tela.dataset.api; // "/clientes" (respeita o context path)
  const COLUNAS = 3;
  const CHAMADOS_POR_PAGINA = 10;

  const lista = document.getElementById('lista');
  const contador = document.getElementById('contador');
  const filtro = document.getElementById('filtro');

  const form = document.getElementById('form-cliente');
  const formTitulo = document.getElementById('modal-cliente-titulo');
  const formErro = document.getElementById('form-erro');
  const campoNome = document.getElementById('nome');
  const campoEmail = document.getElementById('email');
  const campoTelefone = document.getElementById('telefone');
  const btnSalvar = document.getElementById('btn-salvar');
  const modalCliente = ui.modal(document.getElementById('modal-cliente'));
  const atualizarContadores = ui.bindCounters(form);

  const chamadosCliente = document.getElementById('chamados-cliente');
  const chamadosLista = document.getElementById('chamados-lista');
  const chamadosTotal = document.getElementById('chamados-total');
  const chamadosVerTodos = document.getElementById('chamados-ver-todos');
  const TELA_CHAMADOS = tela.dataset.telaChamados; // /telas/chamados
  const modalChamados = ui.modal(document.getElementById('modal-chamados'));

  const excluirNome = document.getElementById('excluir-nome');
  const excluirErro = document.getElementById('excluir-erro');
  const btnConfirmarExclusao = document.getElementById('btn-confirmar-exclusao');
  const modalExcluir = ui.modal(document.getElementById('modal-excluir'));

  let clientes = [];
  let emEdicao = null;    // cliente sendo editado (null = criando)
  let emExclusao = null;  // cliente aguardando confirmação de exclusão
  let pedidoChamados = 0; // ignora respostas antigas se outro cliente for aberto antes

  // ---------------- Telefone ----------------

  // Formata no padrão brasileiro enquanto digita: (47) 3333-4444 ou (47) 99999-9999.
  // Números com mais de 11 dígitos (ex.: internacionais) ficam como foram digitados.
  function formatarTelefone(valor) {
    const d = valor.replace(/\D/g, '');
    if (d.length === 0 || d.length > 11) return valor;
    if (d.length <= 2) return '(' + d;
    if (d.length <= 6) return '(' + d.slice(0, 2) + ') ' + d.slice(2);
    if (d.length <= 10) return '(' + d.slice(0, 2) + ') ' + d.slice(2, 6) + '-' + d.slice(6);
    return '(' + d.slice(0, 2) + ') ' + d.slice(2, 7) + '-' + d.slice(7);
  }

  campoTelefone.addEventListener('input', (e) => {
    // Apagando: não reformata, senão o usuário não consegue remover "(" ou "-".
    if (e.inputType && e.inputType.startsWith('delete')) return;
    campoTelefone.value = formatarTelefone(campoTelefone.value);
  });

  // ---------------- Listagem ----------------

  async function carregar() {
    ui.tableMessage(lista, COLUNAS, 'Carregando clientes…', { loading: true });
    contador.textContent = '';
    try {
      clientes = await api.get(API);
      clientes.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
      renderizar();
    } catch (e) {
      ui.tableMessage(lista, COLUNAS, e.message, { error: true, onRetry: carregar });
    }
  }

  function renderizar() {
    const texto = filtro.value.trim();
    const termo = ui.normalize(texto);
    const digitos = texto.replace(/\D/g, '');

    const visiveis = termo
      ? clientes.filter((c) =>
          ui.normalize(c.nome + ' ' + c.email).includes(termo) ||
          // Telefone: compara só os números, então "4799" acha "(47) 99999-9999".
          (digitos.length >= 3 && (c.telefone || '').replace(/\D/g, '').includes(digitos)))
      : clientes;

    contador.textContent = termo
      ? visiveis.length + ' de ' + clientes.length
      : clientes.length + (clientes.length === 1 ? ' cliente' : ' clientes');

    if (clientes.length === 0) {
      ui.tableMessage(lista, COLUNAS, 'Nenhum cliente cadastrado ainda. Clique em “Novo cliente” para começar.');
      return;
    }
    if (visiveis.length === 0) {
      ui.tableMessage(lista, COLUNAS, 'Nenhum cliente encontrado para “' + texto + '”.');
      return;
    }

    lista.replaceChildren(...visiveis.map(linha));
  }

  function linha(cliente) {
    const tr = document.createElement('tr');

    const tdCliente = document.createElement('td');
    const nome = document.createElement('div');
    nome.className = 't-title';
    nome.textContent = cliente.nome;
    const email = document.createElement('div');
    email.className = 't-meta';
    email.textContent = cliente.email;
    tdCliente.append(nome, email);
    // No celular a coluna "Telefone" some; o número aparece aqui, abaixo do e-mail.
    if (cliente.telefone) {
      const tel = document.createElement('div');
      tel.className = 't-meta show-sm';
      tel.textContent = cliente.telefone;
      tdCliente.appendChild(tel);
    }

    const tdTelefone = document.createElement('td');
    tdTelefone.className = 'hide-sm t-nowrap' + (cliente.telefone ? '' : ' t-vazio');
    tdTelefone.textContent = cliente.telefone || 'Não informado';

    const tdAcoes = document.createElement('td');
    tdAcoes.className = 't-actions';
    const acoes = document.createElement('div');
    acoes.className = 'actions';
    acoes.append(
      ui.actionButton({
        text: 'Chamados', icon: 'i-ticket', variant: 'btn-ghost',
        label: 'Ver chamados de ' + cliente.nome, onClick: () => abrirChamados(cliente),
      }),
      ui.actionButton({
        text: 'Editar', icon: 'i-edit', variant: 'btn-ghost',
        label: 'Editar cliente ' + cliente.nome, onClick: () => abrirFormulario(cliente),
      }),
      ui.actionButton({
        text: 'Excluir', icon: 'i-trash', variant: 'btn-danger-ghost',
        label: 'Excluir cliente ' + cliente.nome, onClick: () => abrirExclusao(cliente),
      })
    );
    tdAcoes.appendChild(acoes);

    tr.append(tdCliente, tdTelefone, tdAcoes);
    return tr;
  }

  filtro.addEventListener('input', () => {
    if (clientes.length) renderizar();
  });

  // ---------------- Chamados do cliente ----------------

  function mensagemChamados(texto, opcoes) {
    opcoes = opcoes || {};
    const li = document.createElement('li');
    li.className = 'empty' + (opcoes.error ? ' empty-error' : '');
    if (opcoes.loading) {
      const spinner = document.createElement('span');
      spinner.className = 'spinner';
      spinner.setAttribute('aria-hidden', 'true');
      li.append(spinner, ' ');
    }
    li.append(texto);
    chamadosLista.replaceChildren(li);
  }

  function itemChamado(chamado) {
    const li = document.createElement('li');
    li.className = 'ticket';

    const id = document.createElement('span');
    id.className = 't-id';
    id.textContent = '#' + chamado.id;

    const corpo = document.createElement('div');
    corpo.className = 'ticket-body';
    const titulo = document.createElement('a');
    titulo.className = 't-title t-link';
    titulo.href = TELA_CHAMADOS + '/' + chamado.id;
    titulo.textContent = chamado.titulo;
    const meta = document.createElement('div');
    meta.className = 't-meta';
    const prioridade = dominio.prioridade(chamado.prioridade);
    const prio = document.createElement('span');
    prio.className = 'prio prio-' + prioridade.css;
    prio.textContent = prioridade.label;
    meta.append(
      (chamado.categoria ? chamado.categoria.nome + ' · ' : ''),
      prio,
      ' · aberto em ' + dominio.data(chamado.dataAbertura)
    );
    corpo.append(titulo, meta);

    const status = dominio.status(chamado.status);
    const badge = document.createElement('span');
    badge.className = 'badge badge-' + status.css;
    badge.textContent = status.label;

    li.append(id, corpo, badge);
    return li;
  }

  async function abrirChamados(cliente) {
    chamadosCliente.textContent = cliente.nome + ' · ' + cliente.email;
    chamadosTotal.textContent = '';
    chamadosVerTodos.href = TELA_CHAMADOS + '?clienteId=' + cliente.id;
    chamadosVerTodos.hidden = true;
    mensagemChamados('Carregando chamados…', { loading: true });
    modalChamados.open();
    const pedido = ++pedidoChamados;

    try {
      const url = API + '/' + cliente.id + '/chamados?size=' + CHAMADOS_POR_PAGINA + '&sort=dataAbertura,desc&sort=id,desc';
      const pagina = await api.get(url);
      if (pedido !== pedidoChamados) return;
      const chamados = pagina.content || [];
      const total = ui.pageInfo(pagina).totalElements;

      if (chamados.length === 0) {
        mensagemChamados('Este cliente ainda não abriu nenhum chamado.');
        return;
      }
      chamadosLista.replaceChildren(...chamados.map(itemChamado));
      chamadosVerTodos.hidden = false;
      chamadosTotal.textContent = total > chamados.length
        ? 'Mostrando os ' + chamados.length + ' mais recentes de ' + total
        : total + (total === 1 ? ' chamado' : ' chamados');
    } catch (err) {
      if (pedido === pedidoChamados) mensagemChamados(err.message, { error: true });
    }
  }

  // ---------------- Criar / editar ----------------

  function abrirFormulario(cliente) {
    emEdicao = cliente;
    form.reset();
    ui.hideError(formErro);
    [campoNome, campoEmail].forEach((c) => c.setCustomValidity(''));
    formTitulo.textContent = cliente ? 'Editar cliente' : 'Novo cliente';
    btnSalvar.textContent = cliente ? 'Salvar alterações' : 'Cadastrar cliente';
    campoNome.value = cliente ? cliente.nome : '';
    campoEmail.value = cliente ? cliente.email : '';
    campoTelefone.value = cliente && cliente.telefone ? cliente.telefone : '';
    atualizarContadores();
    modalCliente.open();
    campoNome.focus();
  }

  [campoNome, campoEmail].forEach((campo) => {
    campo.addEventListener('input', () => campo.setCustomValidity(''));
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const dados = {
      nome: campoNome.value.trim(),
      email: campoEmail.value.trim(),
      telefone: campoTelefone.value.trim() || null, // opcional: vazio vira null
    };

    // "required" aceita só espaços; a API não. Barra aqui para não ir ao servidor.
    if (!dados.nome) campoNome.setCustomValidity('Informe o nome.');
    if (!form.reportValidity()) return;

    ui.hideError(formErro);
    btnSalvar.disabled = true;
    try {
      if (emEdicao) {
        await api.put(API + '/' + emEdicao.id, dados);
        ui.toast('Cliente “' + dados.nome + '” atualizado.');
      } else {
        await api.post(API, dados);
        ui.toast('Cliente “' + dados.nome + '” cadastrado.');
      }
      modalCliente.close();
      carregar();
    } catch (err) {
      ui.showError(formErro, err);
      // 409: e-mail já usado por outro cliente. Marca o campo até ser alterado.
      if (err.status === 409) {
        campoEmail.setCustomValidity(err.message);
        campoEmail.focus();
      }
      // 404 na edição: o cliente sumiu (excluído em outra aba); atualiza a lista.
      if (err.status === 404) carregar();
    } finally {
      btnSalvar.disabled = false;
    }
  });

  document.getElementById('btn-novo').addEventListener('click', () => abrirFormulario(null));

  // ---------------- Excluir ----------------

  function abrirExclusao(cliente) {
    emExclusao = cliente;
    excluirNome.textContent = cliente.nome;
    ui.hideError(excluirErro);
    btnConfirmarExclusao.disabled = false;
    modalExcluir.open();
    btnConfirmarExclusao.focus();
  }

  btnConfirmarExclusao.addEventListener('click', async () => {
    if (!emExclusao) return;
    btnConfirmarExclusao.disabled = true;
    try {
      await api.delete(API + '/' + emExclusao.id);
      ui.toast('Cliente “' + emExclusao.nome + '” excluído.');
      modalExcluir.close();
      carregar();
    } catch (err) {
      // 409: cliente com chamados ou comentários registrados. A mensagem vem pronta da API.
      if (err.status === 409) {
        err.detalhes = ['Clientes que já abriram chamados não podem ser excluídos — o histórico de atendimento seria perdido.'];
      }
      ui.showError(excluirErro, err);
      // Em uso: não adianta tentar de novo. Outros erros (rede etc.): permite repetir.
      btnConfirmarExclusao.disabled = err.status === 409;
      if (err.status === 404) carregar();
    }
  });

  carregar();
})();
