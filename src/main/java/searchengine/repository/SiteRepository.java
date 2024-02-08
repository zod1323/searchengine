package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SitePage;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SitePage, Long> {
    SitePage findByUrl(String url);

    List<SitePage> findById(long id);
}
