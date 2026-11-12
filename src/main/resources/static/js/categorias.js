/* Tela de categorias: listagem, filtro, criação, edição e exclusão via API REST (/categorias). */
(function () {
  const tela = document.getElementById('tela-categorias');
  const API = tela.dataset.api; // "/categorias" (respeita o context path)
  const COLUNAS = 3;

  const lista = document.getElementById('lista');
  const contador = document.getElementById('contador');
  const filtro = document.getElementById('filtro');

  const form = document.getElementById('form-categoria');
  const formTitulo = document.getElementById('modal-categoria-titulo');
  const formErro = document.getElementById('form-erro');
  const campoNome = document.getElementById('nome');
  const campoDescricao = document.getElementById('descricao');
  const btnSalvar = document.getElementById('btn-salvar');
  const modalCategoria = ui.modal(document.getElementById('modal-categoria'));
  const atualizarContadores = ui.bindCounters(form);

  const excluirNome = document.getElementById('excluir-nome');
  const excluirErro = document.getElementById('excluir-erro');
  const btnConfirmarExclusao = document.getElementById('btn-confirmar-exclusao');
  const modalExcluir = ui.modal(document.getElementById('modal-excluir'));

  let categorias = [];
  let emEdicao = null;    // categoria sendo editada (null = criando)
  let emExclusao = null;  // categoria aguardando confirmação de exclusão

  // ---------------- Listagem ----------------

  async function carregar() {
    ui.tableMessage(lista, COLUNAS, 'Carregando categorias…', { loading: true });
    contador.textContent = '';
    try {
      categorias = await api.get(API);
      categorias.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
      renderizar();
    } catch (e) {
      ui.tableMessage(lista, COLUNAS, e.message, { error: true, onRetry: carregar });
    }
  }

  function renderizar() {
    const termo = ui.normalize(filtro.value.trim());
    const visiveis = termo
      ? categorias.filter((c) => ui.normalize(c.nome + ' ' + c.descricao).includes(termo))
      : categorias;

    contador.textContent = termo
      ? visiveis.length + ' de ' + categorias.length
      : categorias.length + (categorias.length === 1 ? ' categoria' : ' categorias');

    if (categorias.length === 0) {
      ui.tableMessage(lista, COLUNAS, 'Nenhuma categoria cadastrada ainda. Clique em “Nova categoria” para começar.');
      return;
    }
    if (visiveis.length === 0) {
      ui.tableMessage(lista, COLUNAS, 'Nenhuma categoria encontrada para “' + filtro.value.trim() + '”.');
      return;
    }

    lista.replaceChildren(...visiveis.map(linha));
  }

  function linha(categoria) {
    const tr = document.createElement('tr');

    const tdNome = document.createElement('td');
    tdNome.className = 't-title';
    tdNome.textContent = categoria.nome;

    const tdDescricao = document.createElement('td');
    tdDescricao.className = 't-desc';
    tdDescricao.textContent = categoria.descricao;

    const tdAcoes = document.createElement('td');
    tdAcoes.className = 't-actions';
    const acoes = document.createElement('div');
    acoes.className = 'actions';
    acoes.append(
      ui.actionButton({
        text: 'Editar', icon: 'i-edit', variant: 'btn-ghost',
        label: 'Editar categoria ' + categoria.nome, onClick: () => abrirFormulario(categoria),
      }),
      ui.actionButton({
        text: 'Excluir', icon: 'i-trash', variant: 'btn-danger-ghost',
        label: 'Excluir categoria ' + categoria.nome, onClick: () => abrirExclusao(categoria),
      })
    );
    tdAcoes.appendChild(acoes);

    tr.append(tdNome, tdDescricao, tdAcoes);
    return tr;
  }

  filtro.addEventListener('input', () => {
    if (categorias.length) renderizar();
  });

  // ---------------- Criar / editar ----------------

  function abrirFormulario(categoria) {
    emEdicao = categoria;
    form.reset();
    ui.hideError(formErro);
    formTitulo.textContent = categoria ? 'Editar categoria' : 'Nova categoria';
    btnSalvar.textContent = categoria ? 'Salvar alterações' : 'Criar categoria';
    campoNome.value = categoria ? categoria.nome : '';
    campoDescricao.value = categoria ? categoria.descricao : '';
    atualizarContadores();
    modalCategoria.open();
    campoNome.focus();
  }

  [campoNome, campoDescricao].forEach((campo) => {
    campo.addEventListener('input', () => campo.setCustomValidity(''));
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const dados = { nome: campoNome.value.trim(), descricao: campoDescricao.value.trim() };

    // "required" aceita só espaços; a API não. Barra aqui para não ir ao servidor.
    if (!dados.nome) campoNome.setCustomValidity('Informe o nome.');
    if (!dados.descricao) campoDescricao.setCustomValidity('Informe a descrição.');
    if (!form.reportValidity()) return;

    ui.hideError(formErro);
    btnSalvar.disabled = true;
    try {
      if (emEdicao) {
        await api.put(API + '/' + emEdicao.id, dados);
        ui.toast('Categoria “' + dados.nome + '” atualizada.');
      } else {
        await api.post(API, dados);
        ui.toast('Categoria “' + dados.nome + '” criada.');
      }
      modalCategoria.close();
      carregar();
    } catch (err) {
      ui.showError(formErro, err);
      // 404 na edição: a categoria sumiu (excluída em outra aba); atualiza a lista.
      if (err.status === 404) carregar();
    } finally {
      btnSalvar.disabled = false;
    }
  });

  document.getElementById('btn-nova').addEventListener('click', () => abrirFormulario(null));

  // ---------------- Excluir ----------------

  function abrirExclusao(categoria) {
    emExclusao = categoria;
    excluirNome.textContent = categoria.nome;
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
      ui.toast('Categoria “' + emExclusao.nome + '” excluída.');
      modalExcluir.close();
      carregar();
    } catch (err) {
      // 409: categoria em uso por algum chamado. A mensagem vem pronta da API.
      if (err.status === 409) {
        err.detalhes = ['Altere a categoria dos chamados que a utilizam antes de excluí-la.'];
      }
      ui.showError(excluirErro, err);
      // Em uso: não adianta tentar de novo. Outros erros (rede etc.): permite repetir.
      btnConfirmarExclusao.disabled = err.status === 409;
      if (err.status === 404) carregar();
    }
  });

  carregar();
})();
