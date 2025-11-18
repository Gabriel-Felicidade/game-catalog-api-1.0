package org.acme;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.RateLimiterHandler;
import io.vertx.ext.web.handler.TimeoutHandler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class RateLimiter {

    // O Vert.x Router é injetado automaticamente pelo Quarkus para adicionar handlers
    @Inject
    Router router;

    void init(@Observes StartupEvent ev) {

        // --- 1. Configuração do Rate Limiter para o path da V1 ---
        // Acesso à API V1 (todos os recursos sob /api/v1)

        // Limite de 5 requisições por segundo (1000ms)
        // 5 * 1000ms = 5000ms (5 segundos para cada 5 requisições, aproximadamente)

        // Cria o handler de Rate Limiter (limite de 5 requisições, a cada 1000 milisegundos)
        RateLimiterHandler rateLimiter = RateLimiterHandler.create(5, 1000);

        // Adiciona o handler ao roteador, protegendo as rotas da V1.
        // O Vert.x cuida dos cabeçalhos X-RateLimit-* e do status 429.
        router.route("/api/v1/*")
                .handler(rateLimiter)
                .failureHandler(context -> {
                    // Captura o erro 429 e retorna uma resposta JSON
                    if (context.statusCode() == 429) {
                        context.response()
                                .setStatusCode(429)
                                .putHeader("Content-Type", "application/json")
                                .end("{\"message\": \"Limite de requisições excedido (429 Too Many Requests). Tente novamente em breve.\"}");
                    } else {
                        context.next();
                    }
                });

        // --- (Opcional) Limite mais alto para a V2, simulando plano Premium/Usuário Autenticado ---
        // Limite de 20 requisições a cada 1000 milisegundos
        RateLimiterHandler premiumRateLimiter = RateLimiterHandler.create(20, 1000);
        router.route("/api/v2/*")
                .handler(premiumRateLimiter)
                .failureHandler(context -> {
                    if (context.statusCode() == 429) {
                        context.response()
                                .setStatusCode(429)
                                .putHeader("Content-Type", "application/json")
                                .end("{\"message\": \"Limite de requisições excedido (429 Too Many Requests). Tente novamente em breve.\"}");
                    } else {
                        context.next();
                    }
                });

    }
}