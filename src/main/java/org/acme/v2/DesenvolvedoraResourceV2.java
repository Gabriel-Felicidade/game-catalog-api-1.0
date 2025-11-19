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
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import org.acme.Desenvolvedora;
import org.acme.FichaTecnica;
import org.acme.IdempotencyService;
import org.acme.Jogo;
import org.acme.SearchDesenvolvedoraResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;


@Path("/v2/desenvolvedoras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Desenvolvedoras V2", description = "Endpoints para o catálogo de desenvolvedoras (Versão 2)")
public class DesenvolvedoraResourceV2 {

    @Inject
    IdempotencyService idempotencyService;

    @GET
    @Path("/search")
    @Operation(
            summary = "Busca desenvolvedoras com paginação e ordenação (V2)",
            description = "Mantém a funcionalidade de busca e paginação."
    )
    @APIResponse(
            responseCode = "200",
            description = "Lista de desenvolvedoras retornada com sucesso",
            content = @Content(
                    schema = @Schema(implementation = SearchDesenvolvedoraResponse.class)
            )
    )
    public Response search(
            @Parameter(description = "Query para buscar por nome ou país de origem")
            @QueryParam("q") String q,

            @Parameter(description = "Campo para ordenação (id, nome, paisDeOrigem)")
            @QueryParam("sort") @DefaultValue("id") String sort,

            @Parameter(description = "Direção da ordenação (asc ou desc)")
            @QueryParam("direction") @DefaultValue("asc") String direction,

            @Parameter(description = "Número da página")
            @QueryParam("page") @DefaultValue("0") int page,

            @Parameter(description = "Quantidade de itens por página")
            @QueryParam("size") @DefaultValue("5") int size
    ){
        // Lógica de busca e paginação (mantida da V1)
        Set<String> allowedSortFields = Set.of("id", "nome", "paisDeOrigem");
        if (!allowedSortFields.contains(sort)) {
            sort = "id";
        }

        Sort sortObj = Sort.by(
                sort,
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending
        );

        int effectivePage = Math.max(page, 0);

        PanacheQuery<Desenvolvedora> query;
        if (q == null || q.isBlank()) {
            query = Desenvolvedora.findAll(sortObj);
        } else {
            query = Desenvolvedora.find(
                    "lower(nome) like ?1 or lower(paisDeOrigem) like ?1",
                    sortObj,
                    "%" + q.toLowerCase() + "%"
            );
        }

        List<Desenvolvedora> desenvolvedoras = query.page(effectivePage, size).list();

        var response = new SearchDesenvolvedoraResponse();
        response.desenvolvedoras = desenvolvedoras;
        response.totalDesenvolvedoras = query.count();
        response.totalPages = query.pageCount();
        response.hasMore = effectivePage < (query.pageCount() - 1);

        if (response.hasMore) {
            // OBS: A URL precisa ser corrigida para apontar para a V2
            String nextPageUrl = String.format("http://localhost:8080/v2/desenvolvedoras/search?q=%s&page=%d&size=%d&sort=%s&direction=%s",
                    (q != null ? q : ""), (effectivePage + 1), size, sort, direction);
            response.nextPage = nextPageUrl;
        } else {
            response.nextPage = "";
        }

        return Response.ok(response).build();
    }

    @GET
    @Operation(summary = "Retorna todas as desenvolvedoras (V2)")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Desenvolvedora.class, type = SchemaType.ARRAY)))
    public Response getAll() {
        return Response.ok(Desenvolvedora.listAll()).build();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Retorna uma desenvolvedora por ID (V2)")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Desenvolvedora.class)))
    @APIResponse(responseCode = "404", description = "Desenvolvedora não encontrada")
    public Response getById(@Parameter(description = "ID da desenvolvedora", required = true) @PathParam("id") long id) {
        Desenvolvedora entity = Desenvolvedora.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Adiciona uma nova desenvolvedora (V2 - Idempotente)", description = "Cria uma nova desenvolvedora. Utiliza Idempotency-Key.")
    @APIResponse(responseCode = "201", description = "Desenvolvedora criada", content = @Content(schema = @Schema(implementation = Desenvolvedora.class)))
    @APIResponse(responseCode = "400", description = "Requisição inválida")
    @APIResponse(responseCode = "409", description = "Conflito - Desenvolvedora com o mesmo nome já existe")
    public Response insert(@Valid Desenvolvedora desenvolvedora,
                           @Parameter(description = "Chave única para garantir a idempotência da requisição.")
                           @HeaderParam("Idempotency-Key") String idempotencyKey) {

        // 1. Lógica de Idempotência
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Response cachedResponse = idempotencyService.getResponse(idempotencyKey);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        // 2. Lógica de Negócio: Validação e Persistência
        if (Desenvolvedora.find("nome", desenvolvedora.nome).count() > 0) {
            Response conflictResponse = Response.status(Response.Status.CONFLICT)
                    .entity("{\"message\": \"Uma desenvolvedora com o nome '" + desenvolvedora.nome + "' já está cadastrada.\"}")
                    .build();

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.cacheResponse(idempotencyKey, conflictResponse);
            }
            return conflictResponse;
        }

        Desenvolvedora.persist(desenvolvedora);
        URI location = UriBuilder.fromPath("/v2/desenvolvedoras/{id}").build(desenvolvedora.id); // URIs de retorno V2
        Response response = Response.created(location).entity(desenvolvedora).build();

        // 3. Cache a Resposta
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.cacheResponse(idempotencyKey, response);
        }

        return response;
    }

    @PUT
    @Path("{id}")
    @Transactional
    @Operation(summary = "Atualiza uma desenvolvedora existente (V2)")
    @APIResponse(responseCode = "200", description = "Desenvolvedora atualizada", content = @Content(schema = @Schema(implementation = Desenvolvedora.class)))
    @APIResponse(responseCode = "404", description = "Desenvolvedora não encontrada")
    public Response update(@PathParam("id") long id, @Valid Desenvolvedora newDesenvolvedora) {
        // Lógica de atualização (mantida da V1)
        Desenvolvedora entity = Desenvolvedora.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        entity.nome = newDesenvolvedora.nome;
        entity.dataDeFundacao = newDesenvolvedora.dataDeFundacao;
        entity.paisDeOrigem = newDesenvolvedora.paisDeOrigem;

        if (newDesenvolvedora.fichaTecnica != null) {
            if (entity.fichaTecnica == null) {
                entity.fichaTecnica = new FichaTecnica();
            }
            entity.fichaTecnica.historia = newDesenvolvedora.fichaTecnica.historia;
            entity.fichaTecnica.principaisJogos = newDesenvolvedora.fichaTecnica.principaisJogos;
            entity.fichaTecnica.premiosEReconhecimentos = newDesenvolvedora.fichaTecnica.premiosEReconhecimentos;
        } else {
            entity.fichaTecnica = null;
        }

        return Response.ok(entity).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    @Operation(summary = "Remove uma desenvolvedora (V2)")
    @APIResponse(responseCode = "204", description = "Desenvolvedora removida")
    @APIResponse(responseCode = "404", description = "Desenvolvedora não encontrada")
    @APIResponse(responseCode = "409", description = "Conflito - Desenvolvedora possui jogos vinculados")
    public Response delete(@PathParam("id") long id) {
        // Lógica de exclusão (mantida da V1)
        Desenvolvedora entity = Desenvolvedora.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        long jogosVinculados = Jogo.count("desenvolvedora.id = ?1", id);
        if (jogosVinculados > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Não é possível deletar a desenvolvedora. Existem " + jogosVinculados + " jogo(s) vinculado(s).")
                    .build();
        }
        Desenvolvedora.deleteById(id);
        return Response.noContent().build();
    }
}