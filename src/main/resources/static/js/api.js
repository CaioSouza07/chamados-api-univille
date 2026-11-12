/*
 * Cliente HTTP simples para a API REST.
 *
 *   const lista = await api.get('/categorias');
 *   await api.post('/categorias', { nome, descricao });
 *
 * Em caso de erro lança ApiError com:
 *   - message:  texto do campo "erro" da API (ErroResposta)
 *   - detalhes: lista do campo "detalhes" (ex.: "nome: Nome é obrigatório")
 *   - status:   código HTTP (0 quando não houve resposta)
 */
(function () {
  class ApiError extends Error {
    constructor(message, detalhes, status) {
      super(message);
      this.name = 'ApiError';
      this.detalhes = detalhes || [];
      this.status = status;
    }
  }

  async function request(method, url, body) {
    const options = { method, headers: { Accept: 'application/json' } };
    if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }

    let response;
    try {
      response = await fetch(url, options);
    } catch (e) {
      throw new ApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está rodando.', [], 0);
    }

    if (response.status === 204) return null;

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const message = (data && data.erro) || 'Erro inesperado (HTTP ' + response.status + ').';
      throw new ApiError(message, data && data.detalhes, response.status);
    }
    return data;
  }

  window.ApiError = ApiError;
  window.api = {
    get: (url) => request('GET', url),
    post: (url, body) => request('POST', url, body),
    put: (url, body) => request('PUT', url, body),
    delete: (url) => request('DELETE', url),
  };
})();
