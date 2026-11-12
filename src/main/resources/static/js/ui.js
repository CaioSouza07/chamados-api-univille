/*
 * Componentes de interface reutilizáveis entre as telas:
 *   ui.icon('i-edit')             -> <svg class="icon"> com o símbolo do sprite
 *   ui.toast('Salvo!')            -> aviso rápido no canto da tela
 *   ui.toast('Falhou', 'error')
 *   ui.showError(el, apiError)    -> preenche um .alert com mensagem + detalhes
 *   ui.hideError(el)
 *   ui.modal(dialogEl)            -> { open(), close() } com fechamento por [data-fechar],
 *                                    clique fora e Esc (nativo do <dialog>)
 *
 * Helpers de tela de cadastro (listagem + formulário):
 *   ui.normalize(texto)           -> minúsculo e sem acentos (para filtros)
 *   ui.tableMessage(tbody, colunas, texto, { loading, error, onRetry })
 *                                 -> linha única de "carregando", "vazio" ou "erro"
 *   ui.actionButton({ text, icon, variant, label, onClick })
 *                                 -> botão pequeno das ações da tabela (vira só ícone no celular)
 *   ui.bindCounters(form)         -> liga os contadores [data-contador="idDoCampo"]; devolve a
 *                                    função para atualizá-los (ex.: depois de preencher o form)
 *
 * Paginação (respostas Page da API):
 *   ui.pageInfo(resposta)         -> { number, size, totalElements, totalPages } (aceita o formato
 *                                    { page: {...} } e o antigo, com os campos na raiz)
 *   ui.pagination(el, info, irPara) -> "Mostrando 1–20 de 57" + botões Anterior/Próxima
 *
 * Aviso que sobrevive a uma troca de página (ex.: excluiu e voltou para a lista):
 *   ui.toastAfterNavigation('Chamado excluído.')
 */
(function () {
  const SVG_NS = 'http://www.w3.org/2000/svg';

  function icon(id) {
    const svg = document.createElementNS(SVG_NS, 'svg');
    svg.setAttribute('class', 'icon');
    svg.setAttribute('aria-hidden', 'true');
    const use = document.createElementNS(SVG_NS, 'use');
    use.setAttribute('href', '#' + id);
    svg.appendChild(use);
    return svg;
  }

  function toast(message, type) {
    const region = document.getElementById('toasts');
    if (!region) return;
    const el = document.createElement('div');
    el.className = 'toast' + (type === 'error' ? ' toast-error' : '');
    el.appendChild(icon(type === 'error' ? 'i-alert' : 'i-check'));
    const text = document.createElement('span');
    text.textContent = message;
    el.appendChild(text);
    region.appendChild(el);
    setTimeout(() => {
      el.classList.add('is-leaving');
      setTimeout(() => el.remove(), 250);
    }, 3500);
  }

  function showError(el, error) {
    el.replaceChildren();
    el.appendChild(icon('i-alert'));
    const box = document.createElement('div');
    const title = document.createElement('strong');
    title.textContent = error.message || 'Algo deu errado.';
    box.appendChild(title);
    const detalhes = error.detalhes || [];
    if (detalhes.length) {
      const list = document.createElement('ul');
      detalhes.forEach((d) => {
        const li = document.createElement('li');
        li.textContent = d;
        list.appendChild(li);
      });
      box.appendChild(list);
    }
    el.appendChild(box);
    el.hidden = false;
  }

  function hideError(el) {
    el.hidden = true;
    el.replaceChildren();
  }

  function modal(dialog) {
    let opener = null;

    function close() {
      if (dialog.open) dialog.close();
    }

    dialog.querySelectorAll('[data-fechar]').forEach((btn) => btn.addEventListener('click', close));

    // Clique no fundo escurecido (fora da caixa) fecha o modal.
    dialog.addEventListener('click', (e) => {
      if (e.target === dialog) close();
    });

    // Devolve o foco a quem abriu o modal.
    dialog.addEventListener('close', () => {
      if (opener && document.contains(opener)) opener.focus();
      opener = null;
    });

    return {
      open() {
        opener = document.activeElement;
        dialog.showModal();
      },
      close,
    };
  }

  function normalize(texto) {
    return String(texto).normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
  }

  function tableMessage(tbody, colunas, texto, opcoes) {
    opcoes = opcoes || {};
    const td = document.createElement('td');
    td.className = 'empty' + (opcoes.error ? ' empty-error' : '');
    td.colSpan = colunas;

    if (opcoes.loading) {
      const spinner = document.createElement('span');
      spinner.className = 'spinner';
      spinner.setAttribute('aria-hidden', 'true');
      td.append(spinner, ' ');
    }
    td.append(texto);

    if (opcoes.error && opcoes.onRetry) {
      const retry = document.createElement('button');
      retry.type = 'button';
      retry.className = 'btn btn-ghost btn-sm';
      retry.append(icon('i-refresh'), 'Tentar novamente');
      retry.addEventListener('click', opcoes.onRetry);
      const wrap = document.createElement('div');
      wrap.className = 'empty-action';
      wrap.appendChild(retry);
      td.appendChild(wrap);
    }

    const tr = document.createElement('tr');
    tr.appendChild(td);
    tbody.replaceChildren(tr);
  }

  function actionButton(opcoes) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn btn-sm ' + opcoes.variant;
    btn.setAttribute('aria-label', opcoes.label);
    btn.title = opcoes.label;
    btn.appendChild(icon(opcoes.icon));
    const span = document.createElement('span');
    span.className = 'btn-label';
    span.textContent = opcoes.text;
    btn.appendChild(span);
    btn.addEventListener('click', opcoes.onClick);
    return btn;
  }

  function bindCounters(form) {
    const contadores = [...form.querySelectorAll('[data-contador]')].map((el) => ({
      el,
      campo: form.querySelector('#' + el.dataset.contador),
    }));

    function atualizar() {
      contadores.forEach(({ el, campo }) => {
        const max = campo.maxLength;
        el.textContent = campo.value.length + '/' + max;
        el.classList.toggle('is-limit', campo.value.length >= max * 0.9);
      });
    }

    contadores.forEach(({ campo }) => campo.addEventListener('input', atualizar));
    return atualizar;
  }

  function pageInfo(resposta) {
    const meta = resposta.page || resposta;
    const itens = (resposta.content || []).length;
    return {
      number: meta.number ?? 0,
      size: meta.size ?? itens,
      totalElements: meta.totalElements ?? itens,
      totalPages: meta.totalPages ?? 1,
    };
  }

  function pagination(el, info, irPara) {
    el.replaceChildren();
    if (info.totalElements === 0) {
      el.hidden = true;
      return;
    }
    el.hidden = false;

    const inicio = info.number * info.size + 1;
    const fim = Math.min(inicio + info.size - 1, info.totalElements);
    const resumo = document.createElement('span');
    resumo.className = 'count';
    resumo.textContent = 'Mostrando ' + inicio + '–' + fim + ' de ' + info.totalElements;

    const nav = document.createElement('div');
    nav.className = 'pagination';
    const botao = (texto, iconId, destino, desabilitado, iconeDepois) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'btn btn-ghost btn-sm';
      b.disabled = desabilitado;
      const label = document.createElement('span');
      label.textContent = texto;
      if (iconeDepois) b.append(label, icon(iconId)); else b.append(icon(iconId), label);
      b.addEventListener('click', () => irPara(destino));
      return b;
    };
    const pagina = document.createElement('span');
    pagina.className = 'pagination-page';
    pagina.textContent = (info.number + 1) + ' de ' + Math.max(info.totalPages, 1);
    nav.append(
      botao('Anterior', 'i-chevron-left', info.number - 1, info.number === 0, false),
      pagina,
      botao('Próxima', 'i-chevron-right', info.number + 1, info.number + 1 >= info.totalPages, true)
    );

    el.append(resumo, nav);
  }

  const CHAVE_TOAST = 'toastPendente';

  function toastAfterNavigation(message, type) {
    try { sessionStorage.setItem(CHAVE_TOAST, JSON.stringify({ message, type })); } catch (e) {}
  }

  // Mostra o aviso deixado pela página anterior (se houver).
  try {
    const pendente = JSON.parse(sessionStorage.getItem(CHAVE_TOAST) || 'null');
    if (pendente) {
      sessionStorage.removeItem(CHAVE_TOAST);
      document.addEventListener('DOMContentLoaded', () => toast(pendente.message, pendente.type));
    }
  } catch (e) {}

  window.ui = {
    icon, toast, showError, hideError, modal, normalize, tableMessage, actionButton, bindCounters,
    pageInfo, pagination, toastAfterNavigation,
  };
})();
