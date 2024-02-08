package searchengine.morphology;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class Analyzer implements Morphology {
    private static RussianLuceneMorphology russianLuceneMorphology;
    private static EnglishLuceneMorphology englishLuceneMorphology;
    private final static String REGEX = "\\p{Punct}|\\d|№|©|◄|«|»|—|-|@|…";
    private final static Marker INVALID_SYMBOL_MARKER = MarkerManager.getMarker("INVALID_SYMBOL");
    private final static Logger LOGGER = LogManager.getLogger(Analyzer.class);


    static {
        try {
            russianLuceneMorphology = new RussianLuceneMorphology();
            englishLuceneMorphology = new EnglishLuceneMorphology();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public HashMap<String, Integer> getLemmaList(String content) {
        content = content.toLowerCase(Locale.ROOT)
                .replaceAll(REGEX, " ");
        HashMap<String, Integer> lemmaList = new HashMap<>();
        List<String> elements = Arrays.stream(content.toLowerCase().split("\\s+")).toList();
        for (String el : elements) {
            List<String> wordsList = getLemma(el);
            for (String word : wordsList) {
                int count = lemmaList.getOrDefault(word, 0);
                lemmaList.put(word, count + 1);
            }

        }
        return lemmaList;
    }

    @Override
    public List<String> getLemma(String word) {
        List<String> lemmaList = new ArrayList<>();
        List<String> baseForm;
        String lang = "ru";
        if (word.matches("[A-z]+")) {
            lang = "en";
        }

        try {
            if (lang.equals("en")) {
                baseForm = englishLuceneMorphology.getNormalForms(word);
            } else {
                baseForm = russianLuceneMorphology.getNormalForms(word);
            }

           // log.info("baseForm.size: " + baseForm.size());
            if (baseForm.size() > 0) {
                lemmaList.addAll(baseForm);
            }

        } catch (Exception e) {
            LOGGER.debug(INVALID_SYMBOL_MARKER, "Символ не найден - " + word);
        }
        return lemmaList;
    }

    @Override
    public List<Integer> findLemmaIndexInText(String content, String lemma) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String el : elements) {
            List<String> lemmas = getLemma(el);
            if (lemmas.contains(lemma)) {
                lemmaIndexList.add(index);
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }


}
