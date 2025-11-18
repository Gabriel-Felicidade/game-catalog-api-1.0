package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço responsável por gerenciar chaves de idempotência em operações POST.
 * Em produção, o cache seria tipicamente feito via Redis ou um banco de dados dedicado.
 */
@ApplicationScoped
public class IdempotencyService {

    // Simula um repositório: armazena a chave e a resposta HTTP da primeira requisição
    private final Map<String, Response> cache = new ConcurrentHashMap<>();

    /**
     * Retorna a resposta armazenada se a chave de idempotência já tiver sido usada.
     * * @param idempotencyKey A chave única fornecida pelo cliente.
     * @return A resposta da requisição original ou null se a chave for nova.
     */
    public Response getResponse(String idempotencyKey) {
        return cache.get(idempotencyKey);
    }

    /**
     * Armazena a chave de idempotência e a resposta HTTP da operação recém-concluída.
     * * @param idempotencyKey A chave única fornecida pelo cliente.
     * @param response A resposta HTTP bem-sucedida (ex: 201 Created).
     */
    public void cacheResponse(String idempotencyKey, Response response) {
        // Armazena a resposta, impedindo que requisições duplicadas sejam processadas.
        cache.putIfAbsent(idempotencyKey, response);
    }
}