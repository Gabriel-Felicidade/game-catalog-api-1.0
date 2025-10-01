package org.acme;

import java.util.ArrayList;
import java.util.List;

public class SearchJogoResponse {
    public List<Jogo> jogos = new ArrayList<>();
    public long totalJogos;
    public int totalPages;
    public boolean hasMore;
    public String nextPage;
}