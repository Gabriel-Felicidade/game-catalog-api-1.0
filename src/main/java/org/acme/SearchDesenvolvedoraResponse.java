package org.acme;

import java.util.ArrayList;
import java.util.List;

public class SearchDesenvolvedoraResponse {
    public List<zDesenvolvedora> desenvolvedoras = new ArrayList<zDesenvolvedora>();
    public long totalDesenvolvedoras;
    public int totalPages;
    public boolean hasMore;
    public String nextPage;
    public long totalItens;
}