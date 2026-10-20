package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.enums.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(

        @NotBlank(message = "titulo é obrigatório")
        @Size(max = 100, message = "titulo deve ter no máximo 100 caracteres")
        String titulo,

        @NotBlank(message = "descricao é obrigatória")
        @Size(max = 254, message = "descricao deve ter no máximo 254 caracteres")
        String descricao,

        @NotNull(message = "prioridade é obrigatória")
        Prioridade prioridade,

        @NotNull(message = "clienteId é obrigatório")
        Long clienteId,

        @NotNull(message = "categoriaId é obrigatória")
        Long categoriaId
) {
}
