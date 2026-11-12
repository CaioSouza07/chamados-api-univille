/* Tela de categorias: listagem, filtro, criação, edição e exclusão via API REST (/categorias). */
(function () {
  const tela = document.getElementById('tela-categorias');
  const API = tela.dataset.api; // "/categorias" (respeita o context path)

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

  const excluirNome = document.getElementById('excluir-nome');
  const excluirErro = document.getElementById('excluir-erro');
  const btnConfirmarExclusao = document.getElementById('btn-confirmar-exclusao');
  const modalExcluir = ui.modal(document.getElementById('modal-excluir'));

  let categorias = [];
  let emEdicao = null;    // categoria sendo editada (null = criando)
  let emExclusao = null;  // categoria aguardando confirmação de exclusão

  // ---------------- Listagem ----------------

  async function carregar() {
    mostrarMensagem('Carregando categorias…', { carregando: true });
    contador.textContent = '';
    try {
      categorias = await api.get(API);
      categorias.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
      renderizar();
    } catch (e) {
      mostrarMensagem(e.message, { erro: true });
    }
  }

  function normalizar(texto) {
    return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }

  function renderizar() {
    const termo = normalizar(filtro.value.trim());
    const visiveis = termo
      ? categorias.filter((c) => normalizar(c.nome + ' ' + c.descricao).includes(termo))
      : categorias;

    contador.textContent = termo
      ? visiveis.length + ' de ' + categorias.length
      : categorias.length + (categorias.length === 1 ? ' categoria' : ' categorias');

    if (categorias.length === 0) {
      mostrarMensagem('Nenhuma categoria cadastrada ainda. Clique em “Nova categoria” para começar.');
      return;
    }
    if (visiveis.length === 0) {
      mostrarMensagem('Nenhuma categoria encontrada para “' + filtro.value.trim() + '”.');
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
      botaoAcao('Editar', 'i-edit', 'btn-ghost', 'Editar categoria ' + categoria.nome, () => abrirFormulario(categoria)),
      botaoAcao('Excluir', 'i-trash', 'btn-danger-ghost', 'Excluir categoria ' + categoria.nome, () => abrirExclusao(categoria))
    );
    tdAcoes.appendChild(acoes);

    tr.append(tdNome, tdDescricao, tdAcoes);
    return tr;
  }

  function botaoAcao(texto, iconId, variante, label, onClick) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn btn-sm ' + variante;
    btn.setAttribute('aria-label', label);
    btn.title = label;
    btn.appendChild(ui.icon(iconId));
    const span = document.createElement('span');
    span.className = 'btn-label';
    span.textContent = texto;
    btn.appendChild(span);
    btn.addEventListener('click', onClick);
    return btn;
  }

  function mostrarMensagem(texto, opcoes) {
    opcoes = opcoes || {};
    const td = document.createElement('td');
    td.className = 'empty' + (opcoes.erro ? ' empty-error' : '');
    td.colSpan = 3;

    if (opcoes.carregando) {
      const spinner = document.createElement('span');
      spinner.className = 'spinner';
      spinner.setAttribute('aria-hidden', 'true');
      td.append(spinner, ' ');
    }
    td.append(texto);

    if (opcoes.erro) {
      const retry = document.createElement('button');
      retry.type = 'button';
      retry.className = 'btn btn-ghost btn-sm';
      retry.append(ui.icon('i-refresh'), 'Tentar novamente');
      retry.addEventListener('click', carregar);
      const wrap = document.createElement('div');
      wrap.className = 'empty-action';
      wrap.appendChild(retry);
      td.appendChild(wrap);
    }

    const tr = document.createElement('tr');
    tr.appendChild(td);
    lista.replaceChildren(tr);
  }

  filtro.addEventListener('input', () => {
    if (categorias.length) renderizar();
  });

  // ---------------- Criar / editar ----------------

  function atualizarContadores() {
    document.querySelectorAll('[data-contador]').forEach((el) => {
      const campo = document.getElementById(el.dataset.contador);
      const max = campo.maxLength;
      el.textContent = campo.value.length + '/' + max;
      el.classList.toggle('is-limit', campo.value.length >= max * 0.9);
    });
  }

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
    campo.addEventListener('input', () => {
      campo.setCustomValidity('');
      atualizarContadores();
    });
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
