package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;


@Path("/desenvolvedoras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DesenvolvedoraResource {

// Adicione este método dentro da classe org.acme.DesenvolvedoraResource

    @GET
    @Path("/search")
    @Operation(
            summary = "Busca desenvolvedoras com paginação e ordenação",
            description = "Retorna uma lista de desenvolvedoras filtrada conforme a pesquisa, com suporte para paginação."
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
        // Validação do campo de ordenação
        Set<String> allowedSortFields = Set.of("id", "nome", "paisDeOrigem");
        if (!allowedSortFields.contains(sort)) {
            sort = "id";
        }

        // Cria o objeto de ordenação do Panache
        Sort sortObj = Sort.by(
                sort,
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending
        );

        // Garante que a página não seja negativa
        int effectivePage = Math.max(page, 0);

        // Cria a query de busca com Panache
        PanacheQuery<Desenvolvedora> query;
        if (q == null || q.isBlank()) {
            query = Desenvolvedora.findAll(sortObj);
        } else {
            // Busca por nome ou país de origem (case-insensitive)
            query = Desenvolvedora.find(
                    "lower(nome) like ?1 or lower(paisDeOrigem) like ?1",
                    sortObj,
                    "%" + q.toLowerCase() + "%"
            );
        }

        // Aplica a paginação
        List<Desenvolvedora> desenvolvedoras = query.page(effectivePage, size).list();

        // Monta o objeto de resposta
        var response = new SearchDesenvolvedoraResponse();
        response.desenvolvedoras = desenvolvedoras;
        response.totalDesenvolvedoras = query.count();
        response.totalPages = query.pageCount();
        response.hasMore = effectivePage < (query.pageCount() - 1);

        if (response.hasMore) {
            String nextPageUrl = String.format("http://localhost:8080/desenvolvedoras/search?q=%s&page=%d&size=%d&sort=%s&direction=%s",
                    (q != null ? q : ""), (effectivePage + 1), size, sort, direction);
            response.nextPage = nextPageUrl;
        } else {
            response.nextPage = "";
        }

        return Response.ok(response).build();
    }

    @GET
    @Operation(summary = "Retorna todas as desenvolvedoras")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Desenvolvedora.class, type = SchemaType.ARRAY)))
    public Response getAll() {
        return Response.ok(Desenvolvedora.listAll()).build();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Retorna uma desenvolvedora por ID")
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
    @Operation(summary = "Adiciona uma nova desenvolvedora")
    @APIResponse(responseCode = "201", description = "Desenvolvedora criada", content = @Content(schema = @Schema(implementation = Desenvolvedora.class)))
    @APIResponse(responseCode = "400", description = "Requisição inválida")
    public Response insert(@Valid Desenvolvedora desenvolvedora) {
        Desenvolvedora.persist(desenvolvedora);
        URI location = URI.create("/desenvolvedoras/" + desenvolvedora.id);
        return Response.created(location).entity(desenvolvedora).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    @Operation(summary = "Atualiza uma desenvolvedora existente")
    @APIResponse(responseCode = "200", description = "Desenvolvedora atualizada", content = @Content(schema = @Schema(implementation = Desenvolvedora.class)))
    @APIResponse(responseCode = "404", description = "Desenvolvedora não encontrada")
    public Response update(@PathParam("id") long id, @Valid Desenvolvedora newDesenvolvedora) {
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
    @Operation(summary = "Remove uma desenvolvedora")
    @APIResponse(responseCode = "204", description = "Desenvolvedora removida")
    @APIResponse(responseCode = "404", description = "Desenvolvedora não encontrada")
    @APIResponse(responseCode = "409", description = "Conflito - Desenvolvedora possui jogos vinculados")
    public Response delete(@PathParam("id") long id) {
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