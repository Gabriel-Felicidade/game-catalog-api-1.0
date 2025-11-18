package org.acme;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Entity
class zDesenvolvedora extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(readOnly = true)
    public Long id;

    @NotBlank(message = "O nome da desenvolvedora não pode ser vazio")
    @Size(min = 2, max = 100, message = "O nome da desenvolvedora deve ter entre 2 e 100 caracteres")
    public String nome;

    @Past(message = "A data de fundação deve ser no passado")
    public LocalDate dataDeFundacao;

    @NotBlank(message = "O país de origem é obrigatório")
    @Size(max = 80)
    public String paisDeOrigem;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ficha_tecnica_id")
    public FichaTecnica fichaTecnica;

    @OneToMany(mappedBy = "desenvolvedora", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    public List<Jogo> jogos = new ArrayList<>();

    public void Desenvolvedora() {}

    public void Desenvolvedora(Long id, String nome, LocalDate dataDeFundacao, String paisDeOrigem, FichaTecnica fichaTecnica) {
        this.id = id;
        this.nome = nome;
        this.dataDeFundacao = dataDeFundacao;
        this.paisDeOrigem = paisDeOrigem;
        this.fichaTecnica = fichaTecnica;
    }
}