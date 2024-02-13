package searchengine.services.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResults;
import searchengine.dto.search.StatisticsSearch;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.morphology.Morphology;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.interfaces.SearchService;
import searchengine.utils.CleanerCode;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final Morphology morphology;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexSearchRepository;
    private final SiteRepository siteRepository;
    @Value("${app.prepositionsRu}")
    private List<String> prepositionsRu;

    @Value("${app.prepositionsEn}")
    private List<String> prepositionsEn;

    @Override
    public SearchResults allSiteSearch(String searchText, int offset, int limit) {
        log.info("Getting results of the search \"" + searchText + "\"");
        List<SitePage> siteList = siteRepository.findAll();
        List<String> textLemmaList = getLemmaFromSearchText(searchText);


        List<Lemma> foundLemmaList = siteList.stream()
                .flatMap(site -> getLemmaListFromSite(textLemmaList, site).stream())
                .collect(Collectors.toList());


        return getSearchResults(offset, limit, textLemmaList, foundLemmaList);
    }

    @Override
    public SearchResults siteSearch(String searchText, String url, int offset, int limit) {
        log.info("Searching for \"" + searchText + "\" in - " + url);
        SitePage siteMain = siteRepository.findByUrl(url);
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        List<Lemma> foundLemmaList = Stream.of(siteMain)
                .flatMap(site -> getLemmaListFromSite(textLemmaList, site).stream())
                .collect(Collectors.toList());

        return getSearchResults(offset, limit, textLemmaList, foundLemmaList);
    }

    private SearchResults getSearchResults(int offset, int limit, List<String> textLemmaList, List<Lemma> foundLemmaList) {
        List<StatisticsSearch> searchData = getSearchDtoList(foundLemmaList, textLemmaList);
        log.info("searchData" + searchData.size());
        searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));

        log.info("Data found.");
        long count = searchData.size();
        return new SearchResults(count > 0, searchData.size() > 0 ? (int) count : 0, getPage(searchData, offset, limit));
    }

    public List<StatisticsSearch> getPage(List<StatisticsSearch> searchData, int offset, int limit) {

        return searchData.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<StatisticsSearch> getSearchDtoList(List<Lemma> lemmaList, List<String> textLemmaList) {

        List<StatisticsSearch> result = new ArrayList<>();
        if (lemmaList.size() >= textLemmaList.size()) {
            List<Page> foundPageList = pageRepository.findByLemmaList(lemmaList);
            List<IndexSearch> foundIndexList = indexSearchRepository.findByPagesAndLemmas(lemmaList, foundPageList);
            Map<Page, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
            return getSearchData(sortedPageByAbsRelevance, textLemmaList);
        } else {
            return result;
        }
    }

    private Map<Page, Float> getPageAbsRelevance(List<Page> pageList, List<IndexSearch> indexList) {
        Map<Page, Float> pageWithRelevance = new HashMap<>();
        for (Page page : pageList) {
            float relevant = 0;
            for (IndexSearch index : indexList) {
                if (index.getPage() == page) {
                    relevant += index.getRank();
                }
            }
            pageWithRelevance.put(page, relevant);
        }

        float maxRelevance = pageWithRelevance.values().stream().max(Float::compare).orElse(0.0f);

        Map<Page, Float> pageWithAbsRelevance = new LinkedHashMap<>();
        for (Map.Entry<Page, Float> entry : pageWithRelevance.entrySet()) {
            float absRelevant = entry.getValue() / maxRelevance;
            pageWithAbsRelevance.put(entry.getKey(), absRelevant);
        }

        return pageWithAbsRelevance.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Page, Float> entry) -> entry.getKey().getPath()).reversed()
                        .thenComparing(Map.Entry.comparingByValue(Comparator.reverseOrder())))
                .distinct()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<StatisticsSearch> getSearchData(Map<Page, Float> pageList, List<String> textLemmaList) {
        List<StatisticsSearch> result = new ArrayList<>();

        for (Map.Entry<Page, Float> entry : pageList.entrySet()) {
            Page page = entry.getKey();
            Float absRelevance = entry.getValue();
            String uri = page.getPath();
            String content = page.getContent();
            SitePage pageSite = page.getSiteId();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();

            String title = CleanerCode.clearing(content, "title");
            String body = CleanerCode.clearing(content, "body");

            String snippet = getSnippet(title + " " + body, textLemmaList);

            result.add(new StatisticsSearch(site, siteName, uri, title, snippet, absRelevance));
        }

        return result;
    }

    private List<String> getLemmaFromSearchText(String searchText) {
        String[] words = searchText.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String lemma : words) {
            if (!isPreposition(lemma)) {
                List<String> list = morphology.getLemma(lemma);
                lemmaList.addAll(list);
            }
        }
        return lemmaList;
    }

    private boolean isPreposition(String word) {
        return prepositionsRu.contains(word) || prepositionsEn.contains(word);
    }

    private List<Lemma> getLemmaListFromSite(List<String> lemmas, SitePage site) {
        return lemmaRepository.findLemmaListBySite(lemmas, site);
    }

    private String getSnippet(String content, List<String> lemmaList) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (String lemma : lemmaList) {
            lemmaIndex.addAll(morphology.findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        for (String word : wordsList) {
            result.append(word).append("... ");
            count++;
            if (count > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        int size = lemmaIndex.size();
        for (int i = 0; i < size; i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < size && lemmaIndex.get(nextPoint) - end > 0 && lemmaIndex.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint++;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {

        String word = (end != -1) ? content.substring(start, end) : content.substring(start);

        int prevPoint = (content.lastIndexOf(" ", start) != -1) ? content.lastIndexOf(" ", start) : start;
        int lastPoint = (content.indexOf(" ", end + 30) != -1) ? content.indexOf(" ", end + 30) : content.indexOf(" ", end);

        String text = (end != -1) ? content.substring(prevPoint, lastPoint) : content.substring(prevPoint);

        try {
            text = text.replaceAll(word, "<b><i>" + word + "</i></b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }

}
