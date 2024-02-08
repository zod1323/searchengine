package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.Response;
import searchengine.dto.search.SearchResults;
import searchengine.dto.statistics.BadRequest;
import searchengine.dto.statistics.StatisticsResponse;

import searchengine.repository.SiteRepository;
import searchengine.services.interfaces.IndexingService;
import searchengine.services.interfaces.SearchService;
import searchengine.services.interfaces.StatisticsService;


@RestController
@Slf4j
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteRepository siteRepository, SearchService searchService) {

        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (indexingService.indexingAll()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new BadRequest(false, "Indexing is started"),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return ResponseEntity.ok(new Response(true));
        } else {
            String errorMessage = "Indexing was not stopped because it was not started";
            return ResponseEntity.badRequest().body(new BadRequest(false, errorMessage));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(name = "query", defaultValue = "") String request,
            @RequestParam(name = "site", defaultValue = "") String site,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        if (request.isBlank()) {
            return ResponseEntity.badRequest().body(new BadRequest(false, "Empty request"));
        }

        SearchResults searchResults;
        if (!site.isBlank()) {
            if (siteRepository.findByUrl(site) == null) {
                return ResponseEntity.badRequest().body(new BadRequest(false, "Required page not found"));
            }
            searchResults = searchService.siteSearch(request, site, offset, limit);
        } else {
            searchResults = searchService.allSiteSearch(request, offset, limit);
        }

        return ResponseEntity.ok(searchResults);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        if (url.isBlank()) {
            log.info("Page not specified");
            return ResponseEntity.badRequest().body(new BadRequest(false, "Page not specified"));
        }

        if (indexingService.urlIndexing(url)) {
            log.info("Page - " + url + " - added for reindexing");
            return ResponseEntity.ok(new Response(true));
        } else {
            log.info("Required page out of configuration file");
            return ResponseEntity.badRequest().body(new BadRequest(false, "Required page out of configuration file"));
        }
    }

}
