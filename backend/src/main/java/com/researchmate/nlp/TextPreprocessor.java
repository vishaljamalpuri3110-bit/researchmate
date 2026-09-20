package com.researchmate.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TextPreprocessor {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]+");

    private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can",
            "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
            "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having",
            "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how",
            "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself",
            "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once",
            "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she",
            "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the",
            "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll",
            "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was",
            "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's",
            "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
            "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves",
            // Academic specific common stopwords
            "et", "al", "fig", "figure", "table", "section", "paper", "propose", "proposed", "author", "authors",
            "study", "results", "shown", "show", "using", "based", "approach", "also", "well", "one", "two", "use",
            "used", "via", "may", "can", "e", "g", "i", "e"
    )));

    public List<String> preprocess(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String[] rawTokens = NON_ALPHANUMERIC.split(text.toLowerCase());
        List<String> filteredTokens = new ArrayList<>(rawTokens.length);

        for (String token : rawTokens) {
            token = token.trim();
            if (token.length() >= 2 && token.length() <= 35 && !STOP_WORDS.contains(token) && !isNumeric(token)) {
                filteredTokens.add(token);
            }
        }

        return filteredTokens;
    }

    private boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
