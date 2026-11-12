/* Menu lateral (recolhível no desktop, gaveta no celular). Usado por todas as telas. */
(function () {
  var root = document.documentElement;
  var toggle = document.getElementById('menu-toggle');          // topo (celular)
  var sidebarToggle = document.getElementById('sidebar-toggle'); // dentro do menu
  var mobileQuery = window.matchMedia('(max-width: 860px)');

  if (!toggle || !sidebarToggle) return;

  function sync() {
    var open = !root.classList.contains('sidebar-collapsed');
    toggle.setAttribute('aria-expanded', String(open));
    sidebarToggle.setAttribute('aria-expanded', String(open));
    var label = open ? 'Ocultar menu lateral' : 'Mostrar menu lateral';
    sidebarToggle.setAttribute('aria-label', label);
    sidebarToggle.title = open ? 'Ocultar menu' : 'Mostrar menu';
  }

  function setCollapsed(collapsed) {
    root.classList.toggle('sidebar-collapsed', collapsed);
    // Só o desktop guarda a preferência; no celular o menu é uma gaveta temporária.
    if (!mobileQuery.matches) {
      try { localStorage.setItem('sidebar', collapsed ? 'closed' : 'open'); } catch (e) {}
    }
    sync();
  }

  function onToggleClick() {
    setCollapsed(!root.classList.contains('sidebar-collapsed'));
  }
  toggle.addEventListener('click', onToggleClick);
  sidebarToggle.addEventListener('click', function () {
    onToggleClick();
    // No celular o menu some ao fechar; devolve o foco ao botão do topo.
    if (mobileQuery.matches) toggle.focus();
  });

  // Gaveta (celular): fecha ao clicar fora, com Esc ou ao escolher um item.
  document.getElementById('backdrop').addEventListener('click', function () { setCollapsed(true); });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && mobileQuery.matches && !root.classList.contains('sidebar-collapsed')) {
      setCollapsed(true);
      toggle.focus();
    }
  });
  document.querySelectorAll('#sidebar .nav-link').forEach(function (link) {
    link.addEventListener('click', function () { if (mobileQuery.matches) setCollapsed(true); });
  });

  // Ao cruzar o breakpoint, volta ao padrão de cada modo.
  mobileQuery.addEventListener('change', function (e) {
    var saved = null;
    try { saved = localStorage.getItem('sidebar'); } catch (err) {}
    root.classList.toggle('sidebar-collapsed', e.matches || saved === 'closed');
    sync();
  });

  sync();
})();
