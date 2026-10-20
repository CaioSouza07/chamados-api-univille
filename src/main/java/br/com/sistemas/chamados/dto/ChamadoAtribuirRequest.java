package br.com.sistemas.chamados.dto;

import jakarta.validation.constraints.NotNull;

public record ChamadoAtribuirRequest(

        @NotNull(message = "tecnicoId é obrigatório")
        Long tecnicoId
) {
}
