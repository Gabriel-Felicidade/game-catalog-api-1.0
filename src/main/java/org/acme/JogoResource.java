package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;


@Path("/jogos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JogoResource {

// Adicione este método dentro da classe org.acme.JogoResource

// Adicione este método dentro da classe org.acme.JogoResource

    @GET
    @Path("/search")
    @Operation(
            summary = "Busca jogos com paginação e ordenação",
            description = "Retorna uma lista de jogos filtrada conforme a pesquisa, com suporte para paginação."
    )
    @APIResponse(
            responseCode = "200",
            description = "Lista de jogos retornada com sucesso",
            content = @Content(
                    schema = @Schema(implementation = SearchJogoResponse.class)
            )
    )
    public Response search(
            @Parameter(description = "Query para buscar por título ou ano de lançamento")
            @QueryParam("q") String q,

            @Parameter(description = "Campo para ordenação (id, titulo, anoLancamento)")
            @QueryParam("sort") @DefaultValue("id") String sort,

            @Parameter(description = "Direção da ordenação (asc ou desc)")
            @QueryParam("direction") @DefaultValue("asc") String direction,

            @Parameter(description = "Número da página")
            @QueryParam("page") @DefaultValue("0") int page,

            @Parameter(description = "Quantidade de itens por página")
            @QueryParam("size") @DefaultValue("5") int size
    ){
        // Validação do campo de ordenação
        Set<String> allowedSortFields = Set.of("id", "titulo", "anoLancamento");
        if (!allowedSortFields.contains(sort)) {
            sort = "id";
        }

        // Cria o objeto de ordenação do Panache
        Sort sortObj = Sort.by(
                sort,
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending
        );

        int effectivePage = Math.max(page, 0);

        // Cria a query de busca com Panache
        PanacheQuery<Jogo> query;
        if (q == null || q.isBlank()) {
            query = Jogo.findAll(sortObj);
        } else {
            try {
                // Tenta converter a query para um número (para buscar por ano)
                int ano = Integer.parseInt(q);
                query = Jogo.find("anoLancamento = ?1", sortObj, ano);
            } catch (NumberFormatException e) {
                // Se não for um número, busca por título
                query = Jogo.find(
                        "lower(titulo) like ?1",
                        sortObj,
                        "%" + q.toLowerCase() + "%"
                );
            }
        }

        // Aplica a paginação
        List<Jogo> jogos = query.page(effectivePage, size).list();

        // Monta o objeto de resposta
        var response = new SearchJogoResponse();
        response.jogos = jogos;
        response.totalJogos = query.count();
        response.totalPages = query.pageCount();
        response.hasMore = effectivePage < (query.pageCount() - 1);

        if (response.hasMore) {
            String nextPageUrl = String.format("http://localhost:8080/jogos/search?q=%s&page=%d&size=%d&sort=%s&direction=%s",
                    (q != null ? q : ""), (effectivePage + 1), size, sort, direction);
            response.nextPage = nextPageUrl;
        } else {
            response.nextPage = "";
        }

        return Response.ok(response).build();
    }

    @GET
    @Operation(summary = "Retorna todos os jogos")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Jogo.class, type = SchemaType.ARRAY)))
    public Response getAll() {
        return Response.ok(Jogo.listAll()).build();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Retorna um jogo por ID")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Jogo.class)))
    @APIResponse(responseCode = "404", description = "Jogo não encontrado")
    public Response getById(@PathParam("id") long id) {
        Jogo entity = Jogo.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Adiciona um novo jogo")
    @APIResponse(responseCode = "201", description = "Jogo criado", content = @Content(schema = @Schema(implementation = Jogo.class)))
    @APIResponse(responseCode = "400", description = "Requisição inválida")
    public Response insert(@Valid Jogo jogo) {
        if (jogo.desenvolvedora != null && jogo.desenvolvedora.id != null) {
            Desenvolvedora d = Desenvolvedora.findById(jogo.desenvolvedora.id);
            if (d == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Desenvolvedora com id " + jogo.desenvolvedora.id + " não existe").build();
            }
            jogo.desenvolvedora = d;
        } else {
            jogo.desenvolvedora = null;
        }

        if (jogo.generos != null && !jogo.generos.isEmpty()) {
            Set<Genero> resolved = new HashSet<>();
            for (Genero g : jogo.generos) {
                if (g == null || g.id == 0) continue;
                Genero fetched = Genero.findById(g.id);
                if (fetched == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Gênero com id " + g.id + " não existe").build();
                }
                resolved.add(fetched);
            }
            jogo.generos = resolved;
        } else {
            jogo.generos = new HashSet<>();
        }

        Jogo.persist(jogo);

        URI location = UriBuilder.fromResource(JogoResource.class).path("{id}").build(jogo.id);
        return Response.created(location).entity(jogo).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    @Operation(summary = "Atualiza um jogo existente")
    @APIResponse(responseCode = "200", description = "Jogo atualizado", content = @Content(schema = @Schema(implementation = Jogo.class)))
    @APIResponse(responseCode = "404", description = "Jogo não encontrado")
    public Response update(@PathParam("id") long id, @Valid Jogo newJogo) {
        Jogo entity = Jogo.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        entity.titulo = newJogo.titulo;
        entity.descricao = newJogo.descricao;
        entity.anoLancamento = newJogo.anoLancamento;
        entity.classificacaoIndicativa = newJogo.classificacaoIndicativa;

        if (newJogo.desenvolvedora != null && newJogo.desenvolvedora.id != null) {
            Desenvolvedora d = Desenvolvedora.findById(newJogo.desenvolvedora.id);
            if (d == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Desenvolvedora com id " + newJogo.desenvolvedora.id + " não existe").build();
            }
            entity.desenvolvedora = d;
        } else {
            entity.desenvolvedora = null;
        }

        if (newJogo.generos != null) {
            Set<Genero> resolved = new HashSet<>();
            for (Genero g : newJogo.generos) {
                if (g == null || g.id == 0) continue;
                Genero fetched = Genero.findById(g.id);
                if (fetched == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Gênero com id " + g.id + " não existe").build();
                }
                resolved.add(fetched);
            }
            entity.generos = resolved;
        } else {
            entity.generos = new HashSet<>();
        }

        return Response.ok(entity).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    @Operation(summary = "Remove um jogo")
    @APIResponse(responseCode = "204", description = "Jogo removido")
    @APIResponse(responseCode = "404", description = "Jogo não encontrado")
    public Response delete(@PathParam("id") long id) {
        Jogo entity = Jogo.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        entity.generos.clear();
        entity.persist();
        Jogo.deleteById(id);
        return Response.noContent().build();
    }

}

