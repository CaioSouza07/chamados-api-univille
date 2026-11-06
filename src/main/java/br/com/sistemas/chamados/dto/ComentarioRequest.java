package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.enums.TipoAutor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(

        @NotBlank(message = "texto é obrigatório")
        @Size(max = 500, message = "texto deve ter no máximo 500 caracteres")
        String texto,

        @NotNull(message = "tipoAutor é obrigatório")
        TipoAutor tipoAutor,

        @NotNull(message = "autorId é obrigatório")
        Long autorId
) {
}
