package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import java.util.List;
import java.util.Set;

@Entity
public class Jogo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(readOnly = true, example = "1")
    public Long id;

    @NotBlank(message = "O título não pode ser vazio")
    @Size(min = 1, max = 200)
    public String titulo;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 2000)
    @Column(length = 2000)
    public String descricao;

    @Min(value = 1950, message = "Ano de lançamento inválido")
    public int anoLancamento;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "A classificação indicativa é obrigatória")
    public ClassificacaoIndicativa classificacaoIndicativa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "desenvolvedora_id")
    public Desenvolvedora desenvolvedora;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "jogo_genero",
            joinColumns = @JoinColumn(name = "jogo_id"),
            inverseJoinColumns = @JoinColumn(name = "genero_id")
    )
    public Set<Genero> generos = new HashSet<>();

    public Jogo() {}

    public Jogo(Long id, String titulo, String descricao, int anoLancamento, ClassificacaoIndicativa classificacaoIndicativa) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.anoLancamento = anoLancamento;
        this.classificacaoIndicativa = classificacaoIndicativa;
    }
}