package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.enums.TipoAutor;

import java.time.LocalDateTime;

public record ComentarioResponse(

        Long id,
        String texto,
        LocalDateTime dataCriacao,
        TipoAutor tipoAutor,
        Long autorId,
        String autorNome
) {
    public static ComentarioResponse de(Comentario comentario) {
        Long autorId;
        String autorNome;
        if (comentario.getTipoAutor() == TipoAutor.CLIENTE) {
            autorId = comentario.getCliente().getId();
            autorNome = comentario.getCliente().getNome();
        } else {
            autorId = comentario.getTecnico().getId();
            autorNome = comentario.getTecnico().getNome();
        }
        return new ComentarioResponse(comentario.getId(), comentario.getTexto(), comentario.getDataCriacao(), comentario.getTipoAutor(), autorId, autorNome);
    }
}
