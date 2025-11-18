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
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject; // NOVO
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;


@Path("/v1/generos") // ALTERADO para V1
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GeneroResource {

    @Inject // NOVO: Injeta o serviço de idempotência
    IdempotencyService idempotencyService;

    @GET
    @Path("/search")
    @Operation(
            summary = "Busca gêneros com paginação e ordenação (V1)",
            description = "Retorna uma lista de gêneros filtrada conforme a pesquisa, com suporte para paginação."
    )
    @APIResponse(
            responseCode = "200",
            description = "Lista de gêneros retornada com sucesso",
            content = @Content(
                    schema = @Schema(implementation = SearchGeneroResponse.class)
            )
    )
    public Response search(
            @Parameter(description = "Query para buscar por nome ou descrição")
            @QueryParam("q") String q,

            @Parameter(description = "Campo para ordenação (id, nome, descricao)")
            @QueryParam("sort") @DefaultValue("id") String sort,

            @Parameter(description = "Direção da ordenação (asc ou desc)")
            @QueryParam("direction") @DefaultValue("asc") String direction,

            @Parameter(description = "Número da página")
            @QueryParam("page") @DefaultValue("0") int page,

            @Parameter(description = "Quantidade de itens por página")
            @QueryParam("size") @DefaultValue("5") int size
    ){
        // Validação do campo de ordenação
        Set<String> allowedSortFields = Set.of("id", "nome", "descricao");
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
        PanacheQuery<Genero> query;
        if (q == null || q.isBlank()) {
            query = Genero.findAll(sortObj);
        } else {
            query = Genero.find(
                    "lower(nome) like ?1 or lower(descricao) like ?1",
                    sortObj,
                    "%" + q.toLowerCase() + "%"
            );
        }

        // Aplica a paginação
        List<Genero> generos = query.page(effectivePage, size).list();

        // Monta o objeto de resposta
        var response = new SearchGeneroResponse();
        response.generos = generos;
        response.totalGeneros = query.count();
        response.totalPages = query.pageCount();
        response.hasMore = effectivePage < (query.pageCount() - 1);

        if (response.hasMore) {
            String nextPageUrl = String.format("http://localhost:8080/v1/generos/search?q=%s&page=%d&size=%d&sort=%s&direction=%s",
                    (q != null ? q : ""), (effectivePage + 1), size, sort, direction);
            response.nextPage = nextPageUrl;
        } else {
            response.nextPage = "";
        }

        return Response.ok(response).build();
    }

    @GET
    @Operation(summary = "Retorna todos os gêneros (V1)")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Genero.class, type = SchemaType.ARRAY)))
    public Response getAll() {
        return Response.ok(Genero.listAll()).build();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Retorna um gênero por ID (V1)")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Genero.class)))
    @APIResponse(responseCode = "404", description = "Gênero não encontrado")
    public Response getById(@Parameter(description = "ID do gênero", required = true) @PathParam("id") long id) {
        Genero entity = Genero.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Adiciona um novo gênero (V1 - Idempotente)", description = "Cria um novo gênero. Utilize o cabeçalho 'Idempotency-Key' para evitar duplicações acidentais (requisito 4.1).")
    @APIResponse(responseCode = "201", description = "Gênero criado", content = @Content(schema = @Schema(implementation = Genero.class)))
    @APIResponse(responseCode = "400", description = "Requisição inválida")
    @APIResponse(responseCode = "409", description = "Conflito - Gênero com o mesmo nome já existe")
    public Response insert(@Valid Genero genero,
                           @Parameter(description = "Chave única para garantir a idempotência da requisição.")
                           @HeaderParam("Idempotency-Key") String idempotencyKey) { // NOVO

        // 1. Lógica de Idempotência: Verifica se a chave já existe no cache
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Response cachedResponse = idempotencyService.getResponse(idempotencyKey);
            if (cachedResponse != null) {
                return cachedResponse; // Retorna a resposta armazenada
            }
        }

        // 2. Lógica de Negócio: Validação e Persistência
        if (Genero.find("nome", genero.nome).count() > 0) {
            Response conflictResponse = Response.status(Response.Status.CONFLICT)
                    .entity("{\"message\": \"Um gênero com o nome '" + genero.nome + "' já está cadastrado.\"}")
                    .build();

            // Cacha a resposta de erro
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.cacheResponse(idempotencyKey, conflictResponse);
            }
            return conflictResponse;
        }

        Genero.persist(genero);
        URI location = UriBuilder.fromResource(GeneroResource.class).path("{id}").build(genero.id);
        Response response = Response.created(location).entity(genero).build();

        // 3. Cache a Resposta
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.cacheResponse(idempotencyKey, response);
        }

        return response;
    }

    @PUT
    @Path("{id}")
    @Transactional
    @Operation(summary = "Atualiza um gênero existente (V1)")
    @APIResponse(responseCode = "200", description = "Gênero atualizado", content = @Content(schema = @Schema(implementation = Genero.class)))
    @APIResponse(responseCode = "404", description = "Gênero não encontrado")
    public Response update(@PathParam("id") long id, @Valid Genero newGenero) {
        Genero entity = Genero.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        entity.nome = newGenero.nome;
        entity.descricao = newGenero.descricao;
        return Response.ok(entity).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    @Operation(summary = "Remove um gênero (V1)")
    @APIResponse(responseCode = "204", description = "Gênero removido")
    @APIResponse(responseCode = "404", description = "Gênero não encontrado")
    @APIResponse(responseCode = "409", description = "Conflito - Gênero possui jogos vinculados")
    public Response delete(@PathParam("id") long id) {
        Genero entity = Genero.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        long jogosVinculados = Jogo.count("?1 MEMBER OF generos", entity);
        if (jogosVinculados > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Não é possível deletar o gênero. Existem " + jogosVinculados + " jogo(s) vinculado(s).")
                    .build();
        }
        Genero.deleteById(id);
        return Response.noContent().build();
    }
}