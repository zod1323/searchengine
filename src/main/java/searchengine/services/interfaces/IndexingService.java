package searchengine.services.interfaces;

public interface IndexingService {
    boolean urlIndexing(String url);

    boolean indexingAll();

    boolean stopIndexing();
}
