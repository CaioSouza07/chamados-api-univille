package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;

public record TecnicoResponse(
        Long id,
        String nome,
        String email,
        Perfil perfil
) {
    public static TecnicoResponse de(Tecnico tecnico) {
        return new TecnicoResponse(tecnico.getId(), tecnico.getNome(), tecnico.getEmail(), tecnico.getPerfil());
    }
}
