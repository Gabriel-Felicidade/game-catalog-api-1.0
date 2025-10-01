package org.acme;

import java.util.ArrayList;
import java.util.List;

public class SearchGeneroResponse {
    public List<Genero> generos = new ArrayList<>();
    public long totalGeneros;
    public int totalPages;
    public boolean hasMore;
    public String nextPage;
}