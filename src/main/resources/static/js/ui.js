/*
 * Componentes de interface reutilizáveis entre as telas:
 *   ui.icon('i-edit')             -> <svg class="icon"> com o símbolo do sprite
 *   ui.toast('Salvo!')            -> aviso rápido no canto da tela
 *   ui.toast('Falhou', 'error')
 *   ui.showError(el, apiError)    -> preenche um .alert com mensagem + detalhes
 *   ui.hideError(el)
 *   ui.modal(dialogEl)            -> { open(), close() } com fechamento por [data-fechar],
 *                                    clique fora e Esc (nativo do <dialog>)
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

  window.ui = { icon, toast, showError, hideError, modal };
})();
