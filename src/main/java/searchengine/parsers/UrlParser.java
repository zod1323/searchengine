package searchengine.parsers;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.statistics.StatisticsPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;


@Slf4j
public class UrlParser extends RecursiveTask<List<StatisticsPage>> {
    private final String address;
    private final List<String> addressList;
    private final List<StatisticsPage> statisticsPageList;

    public UrlParser(String address, List<StatisticsPage> statisticsPageList, List<String> addressList) {
        this.address = address;
        this.statisticsPageList = statisticsPageList;
        this.addressList = addressList;
    }

    public Document getConnect(String url) {
        Document document = null;
        try {
            int CONNECTION_DELAY = 500;
            Thread.sleep(CONNECTION_DELAY);
            document = Jsoup.connect(url)
                    .userAgent(UserAgent.getUserAgent())
                    .referrer("https://www.google.com")
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Thread interrupted while sleeping");
        } catch (IOException e) {
            log.debug("Can't get connected to the site " + url, e);
        }
        return document;
    }

    @Override
    protected List<StatisticsPage> compute() {
        try {
            Thread.sleep(100);
            Document document = getConnect(address);
            String html = document.outerHtml();
            Connection.Response response = document.connection().response();
            int statusCode = response.statusCode();
            if (statusCode == 404 || (statusCode >= 500 && statusCode <= 526)) {
                throw new InterruptedException();
            }
            StatisticsPage statisticsPage = new StatisticsPage(address, html, statusCode);
            statisticsPageList.add(statisticsPage);
            Elements elements = document.select("body").select("a");
            List<UrlParser> taskList = new ArrayList<>();
            for (Element el : elements) {
                String link = el.attr("abs:href");

                if (link.startsWith(el.baseUri()) && !link.equals(el.baseUri()) && !link.contains("#") && !link.contains(".pdf") && !link.contains(".jpg") && !link.contains(".JPG") && !link.contains(".png") && !link.contains(".doc") && !link.contains(".docx") && !link.contains(".xls") && !link.contains(".xlsx") && !addressList.contains(link)) {

                    addressList.add(link);
                    UrlParser task = new UrlParser(link, statisticsPageList, addressList);
                    task.fork();
                    taskList.add(task);
                }
            }
            taskList.forEach(ForkJoinTask::join);
        } catch (Exception e) {
            log.debug("Parsing error - " + address);
            StatisticsPage statisticsPage = new StatisticsPage(address, "", 500);
            statisticsPageList.add(statisticsPage);
        }
        return statisticsPageList;
    }

}