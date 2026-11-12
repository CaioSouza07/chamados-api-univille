/*
 * Modal de criar/editar chamado (fragments/chamado-form.html), compartilhado pela lista e pelo detalhe.
 *
 *   chamadoForm.abrir({ onSalvo: (chamado) => ... })             -> novo chamado
 *   chamadoForm.abrir({ chamado, onSalvo: (chamado) => ... })    -> editar (cliente fica travado)
 */
window.chamadoForm = (function () {
  const dialog = document.getElementById('modal-chamado');
  if (!dialog) return null;

  const urls = dialog.dataset; // apiChamados, apiClientes, apiCategorias, telaClientes, telaCategorias
  const form = document.getElementById('form-chamado');
  const titulo = document.getElementById('modal-chamado-titulo');
  const erro = document.getElementById('chamado-form-erro');
  const campoTitulo = document.getElementById('chamado-titulo');
  const campoDescricao = document.getElementById('chamado-descricao');
  const campoCliente = document.getElementById('chamado-cliente');
  const campoCategoria = document.getElementById('chamado-categoria');
  const hintCliente = document.getElementById('chamado-cliente-hint');
  const btnSalvar = document.getElementById('btn-salvar-chamado');
  const modal = ui.modal(dialog);
  const atualizarContadores = ui.bindCounters(form);

  let emEdicao = null;
  let aoSalvar = null;
  let opcoes = null; // Promise com [clientes, categorias], carregada uma vez por página

  function carregarOpcoes() {
    if (!opcoes) {
      opcoes = Promise.all([api.get(urls.apiClientes), api.get(urls.apiCategorias)]).catch((e) => {
        opcoes = null; // permite tentar de novo na próxima abertura
        throw e;
      });
    }
    return opcoes;
  }

  function preencherSelect(select, itens, placeholder, selecionado) {
    const vazio = new Option(placeholder, '');
    const ordenados = [...itens].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
    select.replaceChildren(vazio, ...ordenados.map((item) => new Option(item.nome, item.id)));
    select.value = selecionado != null ? String(selecionado) : '';
  }

  // Sem cliente ou categoria cadastrados não dá para abrir chamado: explica e leva ao cadastro.
  function avisarFaltaDeCadastro(faltaCliente, faltaCategoria) {
    erro.replaceChildren();
    erro.appendChild(ui.icon('i-alert'));
    const box = document.createElement('div');
    const texto = document.createElement('strong');
    texto.textContent = 'Para abrir um chamado, cadastre antes pelo menos um cliente e uma categoria.';
    box.appendChild(texto);
    const lista = document.createElement('ul');
    [[faltaCliente, urls.telaClientes, 'Cadastrar cliente'], [faltaCategoria, urls.telaCategorias, 'Cadastrar categoria']]
      .filter(([falta]) => falta)
      .forEach(([, href, rotulo]) => {
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.className = 'link';
        a.href = href;
        a.textContent = rotulo;
        li.appendChild(a);
        lista.appendChild(li);
      });
    box.appendChild(lista);
    erro.appendChild(box);
    erro.hidden = false;
  }

  async function abrir(opcoesAbertura) {
    opcoesAbertura = opcoesAbertura || {};
    emEdicao = opcoesAbertura.chamado || null;
    aoSalvar = opcoesAbertura.onSalvo || null;

    form.reset();
    ui.hideError(erro);
    [campoTitulo, campoDescricao].forEach((c) => c.setCustomValidity(''));
    titulo.textContent = emEdicao ? 'Editar chamado #' + emEdicao.id : 'Novo chamado';
    btnSalvar.textContent = emEdicao ? 'Salvar alterações' : 'Abrir chamado';
    campoTitulo.value = emEdicao ? emEdicao.titulo : '';
    campoDescricao.value = emEdicao ? emEdicao.descricao : '';
    form.elements.prioridade.value = emEdicao ? emEdicao.prioridade : 'MEDIA';
    // Trocar o cliente moveria a conversa para outra pessoa; na edição ele fica travado.
    campoCliente.disabled = Boolean(emEdicao);
    hintCliente.hidden = !emEdicao;
    atualizarContadores();

    [campoCliente, campoCategoria].forEach((s) => s.replaceChildren(new Option('Carregando…', '')));
    btnSalvar.disabled = true;
    modal.open();
    campoTitulo.focus();

    try {
      const [clientes, categorias] = await carregarOpcoes();
      preencherSelect(campoCliente, clientes, 'Selecione o cliente', emEdicao && emEdicao.cliente.id);
      preencherSelect(campoCategoria, categorias, 'Selecione a categoria', emEdicao && emEdicao.categoria.id);
      if (!clientes.length || !categorias.length) {
        avisarFaltaDeCadastro(!clientes.length, !categorias.length);
        return;
      }
      btnSalvar.disabled = false;
    } catch (e) {
      ui.showError(erro, e);
    }
  }

  [campoTitulo, campoDescricao].forEach((campo) => {
    campo.addEventListener('input', () => campo.setCustomValidity(''));
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const dados = {
      titulo: campoTitulo.value.trim(),
      descricao: campoDescricao.value.trim(),
      prioridade: form.elements.prioridade.value,
      clienteId: Number(campoCliente.value),
      categoriaId: Number(campoCategoria.value),
    };

    // "required" aceita só espaços; a API não. Barra aqui para não ir ao servidor.
    if (!dados.titulo) campoTitulo.setCustomValidity('Informe o título.');
    if (!dados.descricao) campoDescricao.setCustomValidity('Descreva o problema.');
    if (!form.reportValidity()) return;

    ui.hideError(erro);
    btnSalvar.disabled = true;
    try {
      const salvo = emEdicao
        ? await api.put(urls.apiChamados + '/' + emEdicao.id, dados)
        : await api.post(urls.apiChamados, dados);
      modal.close();
      ui.toast(emEdicao ? 'Chamado #' + salvo.id + ' atualizado.' : 'Chamado #' + salvo.id + ' aberto.');
      if (aoSalvar) aoSalvar(salvo);
    } catch (err) {
      ui.showError(erro, err);
      // 404: cliente/categoria removidos em outra aba; recarrega as opções na próxima vez.
      if (err.status === 404) opcoes = null;
    } finally {
      btnSalvar.disabled = false;
    }
  });

  return { abrir };
})();
