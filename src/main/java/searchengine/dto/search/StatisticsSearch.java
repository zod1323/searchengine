package searchengine.dto.search;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class StatisticsSearch {
    private String address;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;

    public StatisticsSearch(String site, String siteName, String uri,
                            String title, String snippet, Float relevance) {
        this.siteName = siteName;
        this.uri = uri;
        this.address = site + this.uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }
}
