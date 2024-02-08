package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsIndex;
import searchengine.dto.statistics.StatisticsLemma;
import searchengine.dto.statistics.StatisticsPage;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexed implements Runnable {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private static final int corsCpu = Runtime.getRuntime().availableProcessors();
    private final IndexRepository indexRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private final String url;
    private final SitesList sitesList;

    private List<StatisticsPage> getPageDtoList() throws InterruptedException {
        if (!Thread.interrupted()) {
            try {
                String urlPattern = url + "/";
                log.info(urlPattern);
                List<StatisticsPage> statisticsPageVector = new Vector<>();
                List<String> urlList = new Vector<>();
                ForkJoinPool forkJoinPool = new ForkJoinPool(corsCpu);
                List<StatisticsPage> pages = forkJoinPool.invoke(new UrlParser(urlPattern, statisticsPageVector, urlList));

                return new CopyOnWriteArrayList<>(pages);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new InterruptedException();
            }
        } else throw new InterruptedException();
    }

    @Override
    public void run() {
        if (siteRepository.findByUrl(url) != null) {
            log.info("Start delete data about site - " + url);
            deleteDataFromSite();
        }
        log.info("Indexing - " + url + " " + getName());
        saveDateSite();
        try {
            List<StatisticsPage> statisticsPageList = getPageDtoList();
            saveData(statisticsPageList);
            getLemmasPage();
            indexingWords();
        } catch (InterruptedException e) {
            log.error("Indexing stopped - " + url);
            errorSite();
        }
    }

    private void getLemmasPage() {
        if (!Thread.interrupted()) {
            SitePage sitePage = siteRepository.findByUrl(url);
            sitePage.setStatusTime(new Date());
            lemmaParser.run(sitePage);
            List<StatisticsLemma> statisticsLemmaList = lemmaParser.getLemmaDtoList();
            List<Lemma> lemmaList = new CopyOnWriteArrayList<>();
            for (StatisticsLemma statisticsLemma : statisticsLemmaList) {
                lemmaList.add(new Lemma(statisticsLemma.getLemma(), statisticsLemma.getFrequency(), sitePage));
            }
            lemmaRepository.flush();
            lemmaRepository.saveAll(lemmaList);
        } else {
            throw new RuntimeException();
        }
    }

    private void saveData(List<StatisticsPage> pages) throws InterruptedException {
        if (!Thread.interrupted()) {

            SitePage site = siteRepository.findByUrl(url);
            for (StatisticsPage page : pages) {
                int first = page.getUrl().indexOf(url) + url.length();
                log.error("url: - " + page.getUrl());
                String format = page.getUrl().substring(first);
                Page pageForSave = new Page(site, format, page.getCode(),
                        page.getContent());

                try {
                    pageRepository.save(pageForSave);
                } catch (Exception e) {
                    log.error("e: - " + e.getMessage());
                }
            }
        } else {
            throw new InterruptedException();
        }
    }

    private void deleteDataFromSite() {
        SitePage sitePage = siteRepository.findByUrl(url);
        sitePage.setStatus(Status.INDEXING);
        sitePage.setName(getName());
        sitePage.setStatusTime(new Date());
        siteRepository.save(sitePage);
        siteRepository.flush();
        siteRepository.delete(sitePage);
    }

    private void indexingWords() throws InterruptedException {
        if (!Thread.interrupted()) {
            try {
                SitePage sitePage = siteRepository.findByUrl(url);
                indexParser.run(sitePage);
                List<StatisticsIndex> statisticsIndexList = indexParser.getIndexList();
                List<IndexSearch> indexList = new ArrayList<>();
                sitePage.setStatusTime(new Date());
                sitePage.setStatus(Status.INDEXED);
                siteRepository.save(sitePage);
                for (StatisticsIndex statisticsIndex : statisticsIndexList) {
                    Page page = pageRepository.getById(statisticsIndex.getPageID());
                    Lemma lemma = lemmaRepository.getById(statisticsIndex.getLemmaID());
                    indexList.add(new IndexSearch(page, lemma, statisticsIndex.getRank()));
                }
                indexRepository.saveAll(indexList);
                log.info("Done indexing - " + url);
            } catch (Exception e) {
                log.error("Error occurred during indexing: " + e.getMessage());
                throw new InterruptedException();
            }
        } else {
            throw new InterruptedException();
        }
    }

    private void saveDateSite() {
        SitePage sitePage = new SitePage();
        sitePage.setUrl(url);
        sitePage.setName(getName());
        sitePage.setStatus(Status.INDEXING);
        sitePage.setStatusTime(new Date());
        siteRepository.flush();
        siteRepository.save(sitePage);
    }

    private String getName() {
        List<Site> sitesList_2 = sitesList.getSites();
        for (Site map : sitesList_2) {
            if (map.getUrl().equals(url)) {
                return map.getName();
            }
        }
        return "";
    }

    private void errorSite() {
        SitePage sitePage = new SitePage();
        sitePage.setUrl(url);
        sitePage.setLastError("FAILED! Indexing stopped");
        sitePage.setStatus(Status.FAILED);
        sitePage.setStatusTime(new Date());
        siteRepository.save(sitePage);
    }
}

