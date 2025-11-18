package org.acme;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

// Uma classe simples para padronizar a resposta de erro
class ErrorResponse {
    public int status;
    public String message;
}

// Este mapper captura qualquer exceção não tratada e a transforma em um 500
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    @APIResponse(
            responseCode = "500",
            description = "Erro interno do servidor. Contate o suporte.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    public Response toResponse(Throwable exception) {
        // Você pode logar a exceção aqui
        System.err.println("Erro não mapeado capturado: " + exception.getMessage());
        exception.printStackTrace();

        ErrorResponse error = new ErrorResponse();
        error.status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        error.message = "Ocorreu um erro inesperado no servidor. Tente novamente mais tarde.";

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
    }
}