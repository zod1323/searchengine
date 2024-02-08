package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.StatisticsLemma;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.morphology.Morphology;
import searchengine.repository.PageRepository;
import searchengine.utils.CleanerCode;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class LemmaCreation implements LemmaParser {
    private final PageRepository pageRepository;
    private final Morphology morphology;
    private List<StatisticsLemma> statisticsLemmaList;

    public List<StatisticsLemma> getLemmaDtoList() {
        return statisticsLemmaList;
    }

    @Override
    public void run(SitePage site) {
        statisticsLemmaList = new CopyOnWriteArrayList<>();
        Iterable<Page> pageList = pageRepository.findAll();
        TreeMap<String, Integer> lemmaList = new TreeMap<>();

        pageList.forEach(page -> {
            String content = page.getContent();
            String body = CleanerCode.clearing(content, "body");
            String title = CleanerCode.clearing(content, "title");
            HashMap<String, Integer> bodyList = morphology.getLemmaList(body);
            HashMap<String, Integer> titleList = morphology.getLemmaList(title);

            bodyList.forEach((word, frequency) -> lemmaList.merge(word, frequency, Integer::sum));
            titleList.forEach((word, frequency) -> lemmaList.merge(word, frequency, Integer::sum));
        });

        lemmaList.forEach((lemma, frequency) -> statisticsLemmaList.add(new StatisticsLemma(lemma, frequency)));
    }


}
