package searchengine.services.interfaces;

import searchengine.dto.search.SearchResults;


public interface SearchService {
    SearchResults allSiteSearch(String text, int offset, int limit);

    SearchResults siteSearch(String searchText, String url, int offset, int limit);
}
