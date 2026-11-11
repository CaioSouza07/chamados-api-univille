package br.com.sistemas.chamados.service;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriaService")
class CategoriaServiceTest {

    private static final Long ID = 1L;
    private static final String NOME = "Hardware";
    private static final String DESCRICAO = "Problemas com equipamentos";

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private static Categoria categoriaComId(Long id, String nome, String descricao) {
        Categoria categoria = new Categoria(nome, descricao);
        // O id é gerado pelo banco e a entidade não expõe setter
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("salva a categoria e retorna o response")
        void salvaCategoria() {
            when(categoriaRepository.save(any(Categoria.class)))
                    .thenAnswer(inv -> {
                        Categoria c = inv.getArgument(0);
                        ReflectionTestUtils.setField(c, "id", ID);
                        return c;
                    });

            CategoriaResponse response = categoriaService.criar(new CategoriaRequest(NOME, DESCRICAO));

            assertThat(response).isEqualTo(new CategoriaResponse(ID, NOME, DESCRICAO));

            ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
            verify(categoriaRepository).save(captor.capture());
            assertThat(captor.getValue())
                    .extracting(Categoria::getNome, Categoria::getDescricao)
                    .containsExactly(NOME, DESCRICAO);
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("converte todas as categorias para response")
        void retornaTodasAsCategorias() {
            when(categoriaRepository.findAll()).thenReturn(List.of(
                    categoriaComId(1L, "Hardware", "Equipamentos"),
                    categoriaComId(2L, "Software", "Programas")));

            assertThat(categoriaService.listar()).containsExactly(
                    new CategoriaResponse(1L, "Hardware", "Equipamentos"),
                    new CategoriaResponse(2L, "Software", "Programas"));
        }

        @Test
        @DisplayName("retorna lista vazia quando não há categorias")
        void retornaListaVazia() {
            when(categoriaRepository.findAll()).thenReturn(List.of());

            assertThat(categoriaService.listar()).isEmpty();
        }
    }

    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("retorna o response quando a categoria existe")
        void retornaCategoriaExistente() {
            when(categoriaRepository.findById(ID)).thenReturn(Optional.of(categoriaComId(ID, NOME, DESCRICAO)));

            assertThat(categoriaService.buscar(ID)).isEqualTo(new CategoriaResponse(ID, NOME, DESCRICAO));
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando a categoria não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(categoriaRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.buscar(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Categoria 1 não encontrado");
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        private final CategoriaRequest request = new CategoriaRequest("Software", "Problemas com programas");

        @Test
        @DisplayName("atualiza nome e descrição da categoria")
        void atualizaDados() {
            Categoria existente = categoriaComId(ID, NOME, DESCRICAO);
            when(categoriaRepository.findById(ID)).thenReturn(Optional.of(existente));

            CategoriaResponse response = categoriaService.atualizar(ID, request);

            assertThat(response).isEqualTo(new CategoriaResponse(ID, "Software", "Problemas com programas"));
            assertThat(existente)
                    .extracting(Categoria::getNome, Categoria::getDescricao)
                    .containsExactly("Software", "Problemas com programas");
            // A entidade é gerenciada pelo JPA (dirty checking), não precisa de save()
            verify(categoriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando a categoria não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(categoriaRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.atualizar(ID, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(categoriaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("excluir")
    class Excluir {

        @Test
        @DisplayName("remove a categoria quando ela existe")
        void removeCategoriaExistente() {
            Categoria existente = categoriaComId(ID, NOME, DESCRICAO);
            when(categoriaRepository.findById(ID)).thenReturn(Optional.of(existente));

            categoriaService.excluir(ID);

            verify(categoriaRepository).delete(existente);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException e não remove quando a categoria não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(categoriaRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.excluir(ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(categoriaRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("buscarEntity")
    class BuscarEntity {

        @Test
        @DisplayName("retorna a entidade quando a categoria existe")
        void retornaEntidade() {
            Categoria existente = categoriaComId(ID, NOME, DESCRICAO);
            when(categoriaRepository.findById(ID)).thenReturn(Optional.of(existente));

            assertThat(categoriaService.buscarEntity(ID)).isSameAs(existente);
            verifyNoMoreInteractions(categoriaRepository);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException com o id na mensagem quando não existe")
        void lancaExcecaoQuandoNaoExiste() {
            when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.buscarEntity(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Categoria 99 não encontrado");
        }
    }
}
