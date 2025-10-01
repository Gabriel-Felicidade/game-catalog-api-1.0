package org.acme;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Entity
public class FichaTecnica extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(readOnly = true)
    public Long id;

    @Size(max = 2000, message = "A história da desenvolvedora não pode ultrapassar 2000 caracteres")
    @Column(length = 2000)
    public String historia;

    @Size(max = 200, message = "Os principais jogos não podem ultrapassar 200 caracteres")
    public String principaisJogos;

    public String premiosEReconhecimentos;

    @OneToOne(mappedBy = "fichaTecnica", fetch = FetchType.LAZY)
    @JsonIgnore
    public Desenvolvedora desenvolvedora;

    public FichaTecnica() {}

    public FichaTecnica(String historia, String principaisJogos, String premiosEReconhecimentos) {
        this.historia = historia;
        this.principaisJogos = principaisJogos;
        this.premiosEReconhecimentos = premiosEReconhecimentos;
    }
}