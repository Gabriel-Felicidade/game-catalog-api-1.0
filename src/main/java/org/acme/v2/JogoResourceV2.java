package org.acme.v2;

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
import jakarta.inject.Inject;
import org.acme.Desenvolvedora;
import org.acme.Genero;
import org.acme.IdempotencyService;
import org.acme.Jogo;
import org.acme.SearchJogoResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;


@Path("/v2/jogos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Jogos V2", description = "Endpoints para o catálogo de jogos (Versão 2)")
public class JogoResourceV2 {

    @Inject
    IdempotencyService idempotencyService;

    @GET
    @Path("/search")
    @Operation(
            summary = "Busca jogos com paginação e ordenação (V2)",
            description = "Mantém a funcionalidade de busca e paginação."
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
        // Lógica de busca e paginação (mantida da V1)
        Set<String> allowedSortFields = Set.of("id", "titulo", "anoLancamento");
        if (!allowedSortFields.contains(sort)) {
            sort = "id";
        }

        Sort sortObj = Sort.by(
                sort,
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending
        );

        int effectivePage = Math.max(page, 0);

        PanacheQuery<Jogo> query;
        if (q == null || q.isBlank()) {
            query = Jogo.findAll(sortObj);
        } else {
            try {
                int ano = Integer.parseInt(q);
                query = Jogo.find("anoLancamento = ?1", sortObj, ano);
            } catch (NumberFormatException e) {
                query = Jogo.find(
                        "lower(titulo) like ?1",
                        sortObj,
                        "%" + q.toLowerCase() + "%"
                );
            }
        }

        List<Jogo> jogos = query.page(effectivePage, size).list();

        var response = new SearchJogoResponse();
        response.jogos = jogos;
        response.totalJogos = query.count();
        response.totalPages = query.pageCount();
        response.hasMore = effectivePage < (query.pageCount() - 1);

        if (response.hasMore) {
            // OBS: A URL precisa ser corrigida para apontar para a V2
            String nextPageUrl = String.format("http://localhost:8080/v2/jogos/search?q=%s&page=%d&size=%d&sort=%s&direction=%s",
                    (q != null ? q : ""), (effectivePage + 1), size, sort, direction);
            response.nextPage = nextPageUrl;
        } else {
            response.nextPage = "";
        }

        return Response.ok(response).build();
    }

    @GET
    @Operation(summary = "Retorna todos os jogos (V2 - Novo filtro de negócio)", description = "Retorna todos os jogos, mas o endpoint foi alterado na V2 para retornar apenas jogos com classificação LIVRE.")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Jogo.class, type = SchemaType.ARRAY)))
    public List<Jogo> listAll() {
        // Novo comportamento para V2: retorna apenas jogos LIVRE
        return Jogo.list("classificacaoIndicativa = 'LIVRE'");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Retorna um jogo por ID (V2)")
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
    @Operation(summary = "Adiciona um novo jogo (V2 - Idempotente)", description = "Cria um novo jogo. Utiliza Idempotency-Key.")
    @APIResponse(responseCode = "201", description = "Jogo criado", content = @Content(schema = @Schema(implementation = Jogo.class)))
    @APIResponse(responseCode = "400", description = "Requisição inválida")
    @APIResponse(responseCode = "409", description = "Conflito - Jogo com o mesmo título já existe")
    public Response insert(@Valid Jogo jogo,
                           @Parameter(description = "Chave única para garantir a idempotência da requisição.")
                           @HeaderParam("Idempotency-Key") String idempotencyKey) {

        // 1. Lógica de Idempotência: Verifica se a chave já existe no cache
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Response cachedResponse = idempotencyService.getResponse(idempotencyKey);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        // 2. Lógica de Negócio: Validação e Persistência
        if (Jogo.find("titulo", jogo.titulo).count() > 0) {
            Response conflictResponse = Response.status(Response.Status.CONFLICT)
                    .entity("{\"message\": \"Um jogo com o título '" + jogo.titulo + "' já está cadastrado.\"}")
                    .build();

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.cacheResponse(idempotencyKey, conflictResponse);
            }
            return conflictResponse;
        }

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
        URI location = UriBuilder.fromPath("/v2/jogos/{id}").build(jogo.id); // URIs de retorno V2
        Response response = Response.created(location).entity(jogo).build();

        // 3. Cache a Resposta
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.cacheResponse(idempotencyKey, response);
        }

        return response;
    }

    @PUT
    @Path("{id}")
    @Transactional
    @Operation(summary = "Atualiza um jogo existente (V2)")
    @APIResponse(responseCode = "200", description = "Jogo atualizado", content = @Content(schema = @Schema(implementation = Jogo.class)))
    @APIResponse(responseCode = "404", description = "Jogo não encontrado")
    public Response update(@PathParam("id") long id, @Valid Jogo newJogo) {
        // Lógica de atualização (mantida da V1)
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
    @Operation(summary = "Remove um jogo (V2)")
    @APIResponse(responseCode = "204", description = "Jogo removido")
    @APIResponse(responseCode = "404", description = "Jogo não encontrado")
    public Response delete(@PathParam("id") long id) {
        // Lógica de exclusão (mantida da V1)
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