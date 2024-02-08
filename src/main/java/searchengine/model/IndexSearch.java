package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "index_search", indexes = {@Index(
        name = "page_id_list", columnList = "page_id"),
        @Index(name = "lemma_id_list", columnList = "lemma_id")})
@NoArgsConstructor
public class IndexSearch implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private Lemma lemma;

    @Column(nullable = false, name = "index_rank")
    private float rank;

    public IndexSearch(Page page, Lemma lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexSearch that = (IndexSearch) o;
        return id == that.id && Float.compare(that.rank, rank) == 0 && page.equals(that.page)
                && lemma.equals(that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, page, lemma, rank);
    }

    @Override
    public String toString() {
        return "IndexEntity{" +
                "id=" + id +
                ", page=" + page +
                ", lemma=" + lemma +
                ", rank=" + rank +
                '}';
    }
}
