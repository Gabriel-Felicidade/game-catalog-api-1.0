package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/management/keys")
@Produces(MediaType.APPLICATION_JSON)
public class KeyManagementResource {

    private static final String API_KEY = "DEV_API_GAME_CATALOG_12345";

    @GET
    @Path("/generate")
    @Operation(summary = "Gera uma chave de API de desenvolvimento (Apenas para demonstração)")
    public Response generateKey() {
        // Em um cenário real, uma chave nova e única seria persistida no banco de dados aqui.
        return Response.ok("{\"api_key\": \"" + API_KEY + "\", \"warning\": \"Esta chave é fixa e apenas para ambiente DEV.\"}")
                .build();
    }

    // Outros endpoints para revogação, listagem, etc. seriam adicionados aqui.
}