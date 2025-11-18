package org.acme.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Filtro de autenticação que verifica o cabeçalho X-API-KEY para rotas protegidas (V1).
 */
@Provider
@Priority(500) // Prioridade para que este filtro seja executado cedo
public class ApiKeyFilter implements ContainerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    // Chave de demonstração. Em produção, deve ser buscada de um BD/Vault.
    private static final String VALID_API_KEY = "DEV_API_GAME_CATALOG_12345";

    // Protege todos os endpoints que contêm "/v1" no caminho.
    private static final String PROTECTED_PATH_SEGMENT = "/v1";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();

        // Aplica o filtro apenas nas rotas da V1
        if (path.contains(PROTECTED_PATH_SEGMENT)) {

            // 1. Captura a Chave
            String apiKey = requestContext.getHeaderString(API_KEY_HEADER);

            // 2. Valida a Chave
            if (apiKey == null || !apiKey.equals(VALID_API_KEY)) {

                // Retorna 401 Unauthorized
                Response unauthorized = Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"status\": 401, \"message\": \"Acesso negado. Chave de API inválida ou ausente no cabeçalho X-API-KEY.\"}")
                        .build();

                requestContext.abortWith(unauthorized);
                return;
            }
        }
    }
}