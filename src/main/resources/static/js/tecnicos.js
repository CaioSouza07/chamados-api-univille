/* Tela de técnicos: listagem, filtros, criação, edição e exclusão via API REST (/tecnicos). */
(function () {
  const tela = document.getElementById('tela-tecnicos');
  const API = tela.dataset.api; // "/tecnicos" (respeita o context path)
  const COLUNAS = 3;

  // Rótulos do enum Perfil (a API devolve o nome do enum).
  const PERFIS = {
    TECNICO: { label: 'Técnico', css: 'tecnico' },
    ADMIN: { label: 'Administrador', css: 'admin' },
  };
  const perfil = (valor) => PERFIS[valor] || { label: valor, css: 'tecnico' };

  const lista = document.getElementById('lista');
  const contador = document.getElementById('contador');
  const filtro = document.getElementById('filtro');
  const filtroPerfil = document.getElementById('filtro-perfil');

  const form = document.getElementById('form-tecnico');
  const formTitulo = document.getElementById('modal-tecnico-titulo');
  const formErro = document.getElementById('form-erro');
  const campoNome = document.getElementById('nome');
  const campoEmail = document.getElementById('email');
  const btnSalvar = document.getElementById('btn-salvar');
  const modalTecnico = ui.modal(document.getElementById('modal-tecnico'));
  const atualizarContadores = ui.bindCounters(form);

  const excluirNome = document.getElementById('excluir-nome');
  const excluirErro = document.getElementById('excluir-erro');
  const btnConfirmarExclusao = document.getElementById('btn-confirmar-exclusao');
  const modalExcluir = ui.modal(document.getElementById('modal-excluir'));

  let tecnicos = [];
  let emEdicao = null;    // técnico sendo editado (null = criando)
  let emExclusao = null;  // técnico aguardando confirmação de exclusão

  // ---------------- Listagem ----------------

  async function carregar() {
    ui.tableMessage(lista, COLUNAS, 'Carregando técnicos…', { loading: true });
    contador.textContent = '';
    try {
      tecnicos = await api.get(API);
      tecnicos.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
      renderizar();
    } catch (e) {
      ui.tableMessage(lista, COLUNAS, e.message, { error: true, onRetry: carregar });
    }
  }

  function renderizar() {
    const termo = ui.normalize(filtro.value.trim());
    const perfilSelecionado = filtroPerfil.value;
    const filtrando = Boolean(termo || perfilSelecionado);

    const visiveis = tecnicos.filter((t) =>
      (!perfilSelecionado || t.perfil === perfilSelecionado) &&
      (!termo || ui.normalize(t.nome + ' ' + t.email).includes(termo))
    );

    contador.textContent = filtrando
      ? visiveis.length + ' de ' + tecnicos.length
      : tecnicos.length + (tecnicos.length === 1 ? ' técnico' : ' técnicos');

    if (tecnicos.length === 0) {
      ui.tableMessage(lista, COLUNAS, 'Nenhum técnico cadastrado ainda. Clique em “Novo técnico” para começar.');
      return;
    }
    if (visiveis.length === 0) {
      ui.tableMessage(lista, COLUNAS, 'Nenhum técnico encontrado com os filtros atuais.');
      return;
    }

    lista.replaceChildren(...visiveis.map(linha));
  }

  function linha(tecnico) {
    const tr = document.createElement('tr');

    const tdTecnico = document.createElement('td');
    const nome = document.createElement('div');
    nome.className = 't-title';
    nome.textContent = tecnico.nome;
    const email = document.createElement('div');
    email.className = 't-meta';
    email.textContent = tecnico.email;
    tdTecnico.append(nome, email);

    const tdPerfil = document.createElement('td');
    const p = perfil(tecnico.perfil);
    const badge = document.createElement('span');
    badge.className = 'badge badge-perfil-' + p.css;
    badge.textContent = p.label;
    tdPerfil.appendChild(badge);

    const tdAcoes = document.createElement('td');
    tdAcoes.className = 't-actions';
    const acoes = document.createElement('div');
    acoes.className = 'actions';
    acoes.append(
      ui.actionButton({
        text: 'Editar', icon: 'i-edit', variant: 'btn-ghost',
        label: 'Editar técnico ' + tecnico.nome, onClick: () => abrirFormulario(tecnico),
      }),
      ui.actionButton({
        text: 'Excluir', icon: 'i-trash', variant: 'btn-danger-ghost',
        label: 'Excluir técnico ' + tecnico.nome, onClick: () => abrirExclusao(tecnico),
      })
    );
    tdAcoes.appendChild(acoes);

    tr.append(tdTecnico, tdPerfil, tdAcoes);
    return tr;
  }

  filtro.addEventListener('input', () => {
    if (tecnicos.length) renderizar();
  });
  filtroPerfil.addEventListener('change', () => {
    if (tecnicos.length) renderizar();
  });

  // ---------------- Criar / editar ----------------

  function abrirFormulario(tecnico) {
    emEdicao = tecnico;
    form.reset();
    ui.hideError(formErro);
    [campoNome, campoEmail].forEach((c) => c.setCustomValidity(''));
    formTitulo.textContent = tecnico ? 'Editar técnico' : 'Novo técnico';
    btnSalvar.textContent = tecnico ? 'Salvar alterações' : 'Cadastrar técnico';
    campoNome.value = tecnico ? tecnico.nome : '';
    campoEmail.value = tecnico ? tecnico.email : '';
    form.elements.perfil.value = tecnico ? tecnico.perfil : 'TECNICO';
    atualizarContadores();
    modalTecnico.open();
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
      perfil: form.elements.perfil.value,
    };

    // "required" aceita só espaços; a API não. Barra aqui para não ir ao servidor.
    if (!dados.nome) campoNome.setCustomValidity('Informe o nome.');
    if (!form.reportValidity()) return;

    ui.hideError(formErro);
    btnSalvar.disabled = true;
    try {
      if (emEdicao) {
        await api.put(API + '/' + emEdicao.id, dados);
        ui.toast('Técnico “' + dados.nome + '” atualizado.');
      } else {
        await api.post(API, dados);
        ui.toast('Técnico “' + dados.nome + '” cadastrado.');
      }
      modalTecnico.close();
      carregar();
    } catch (err) {
      ui.showError(formErro, err);
      // 409: e-mail já usado por outro técnico. Marca o campo até ser alterado.
      if (err.status === 409) {
        campoEmail.setCustomValidity(err.message);
        campoEmail.focus();
      }
      // 404 na edição: o técnico sumiu (excluído em outra aba); atualiza a lista.
      if (err.status === 404) carregar();
    } finally {
      btnSalvar.disabled = false;
    }
  });

  document.getElementById('btn-novo').addEventListener('click', () => abrirFormulario(null));

  // ---------------- Excluir ----------------

  function abrirExclusao(tecnico) {
    emExclusao = tecnico;
    excluirNome.textContent = tecnico.nome;
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
      ui.toast('Técnico “' + emExclusao.nome + '” excluído.');
      modalExcluir.close();
      carregar();
    } catch (err) {
      // 409: técnico atribuído a chamados ou autor de comentários. A mensagem vem pronta da API.
      if (err.status === 409) {
        err.detalhes = ['Reatribua os chamados deste técnico antes de excluí-lo. Técnicos que já comentaram em chamados não podem ser excluídos.'];
      }
      ui.showError(excluirErro, err);
      // Em uso: não adianta tentar de novo. Outros erros (rede etc.): permite repetir.
      btnConfirmarExclusao.disabled = err.status === 409;
      if (err.status === 404) carregar();
    }
  });

  carregar();
})();
