package com.researchmate.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.researchmate.ai.EmbeddingService;
import com.researchmate.dto.response.ExperimentResultResponse;
import com.researchmate.dto.response.ExperimentResultResponse.MethodMetrics;
import com.researchmate.dto.response.ExperimentResultResponse.QueryEvaluationDetail;
import com.researchmate.dto.response.ExperimentResultResponse.RankedItem;
import com.researchmate.nlp.TextPreprocessor;
import com.researchmate.nlp.TfIdfService;

@Service
public class ExperimentService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);

    private final TfIdfService tfIdfService;
    private final TextPreprocessor textPreprocessor;
    private final EmbeddingService embeddingService;

    public ExperimentService(
            TfIdfService tfIdfService,
            TextPreprocessor textPreprocessor,
            EmbeddingService embeddingService) {
        this.tfIdfService = tfIdfService;
        this.textPreprocessor = textPreprocessor;
        this.embeddingService = embeddingService;
    }

    public ExperimentResultResponse runEvaluation(int k) {
        if (k <= 0) k = 3;

        List<BenchmarkPaper> corpus = loadBenchmarkCorpus();
        List<BenchmarkQuery> queries = loadBenchmarkQueries();

        // 1. Evaluate TF-IDF
        long startTfIdf = System.currentTimeMillis();
        List<List<String>> tokenizedCorpus = new ArrayList<>();
        for (BenchmarkPaper p : corpus) {
            tokenizedCorpus.add(textPreprocessor.preprocess(p.title() + " " + p.abstractText()));
        }
        Map<String, Double> idfMap = tfIdfService.computeIdf(tokenizedCorpus);

        List<Map<String, Double>> tfIdfVectors = new ArrayList<>();
        for (List<String> tokens : tokenizedCorpus) {
            tfIdfVectors.add(tfIdfService.computeTfIdfVector(tokens, idfMap));
        }
        long tfIdfTime = System.currentTimeMillis() - startTfIdf;

        // 2. Evaluate Embeddings
        long startEmbed = System.currentTimeMillis();
        List<float[]> embeddingVectors = new ArrayList<>();
        for (BenchmarkPaper p : corpus) {
            embeddingVectors.add(embeddingService.generateEmbedding(p.title() + " " + p.abstractText()));
        }
        long embedTime = System.currentTimeMillis() - startEmbed;

        // Metrics accumulators
        double totalPrecisionTfIdf = 0.0;
        double totalRecallTfIdf = 0.0;
        double totalApTfIdf = 0.0;
        double totalMrrTfIdf = 0.0;

        double totalPrecisionEmbed = 0.0;
        double totalRecallEmbed = 0.0;
        double totalApEmbed = 0.0;
        double totalMrrEmbed = 0.0;

        List<QueryEvaluationDetail> breakdown = new ArrayList<>();

        for (BenchmarkQuery query : queries) {
            int qIndex = query.corpusIndex();
            BenchmarkPaper qPaper = corpus.get(qIndex);
            Set<Integer> relevantIndices = query.relevantIndices();

            // Rank with TF-IDF
            Map<String, Double> qTfIdf = tfIdfVectors.get(qIndex);
            List<ScoredDoc> rankedTfIdf = new ArrayList<>();
            for (int i = 0; i < corpus.size(); i++) {
                if (i == qIndex) continue; // Exclude self
                double sim = tfIdfService.cosineSimilarity(qTfIdf, tfIdfVectors.get(i));
                rankedTfIdf.add(new ScoredDoc(i, corpus.get(i).title(), sim, relevantIndices.contains(i)));
            }
            rankedTfIdf.sort(Comparator.comparing(ScoredDoc::score).reversed());

            // Rank with Embeddings
            float[] qEmbed = embeddingVectors.get(qIndex);
            List<ScoredDoc> rankedEmbed = new ArrayList<>();
            for (int i = 0; i < corpus.size(); i++) {
                if (i == qIndex) continue;
                double sim = embeddingService.cosineSimilarity(qEmbed, embeddingVectors.get(i));
                rankedEmbed.add(new ScoredDoc(i, corpus.get(i).title(), sim, relevantIndices.contains(i)));
            }
            rankedEmbed.sort(Comparator.comparing(ScoredDoc::score).reversed());

            // Compute metrics for this query
            Metrics qMetricsTfIdf = computeMetricsForQuery(rankedTfIdf, relevantIndices.size(), k);
            Metrics qMetricsEmbed = computeMetricsForQuery(rankedEmbed, relevantIndices.size(), k);

            totalPrecisionTfIdf += qMetricsTfIdf.precision;
            totalRecallTfIdf += qMetricsTfIdf.recall;
            totalApTfIdf += qMetricsTfIdf.averagePrecision;
            totalMrrTfIdf += qMetricsTfIdf.reciprocalRank;

            totalPrecisionEmbed += qMetricsEmbed.precision;
            totalRecallEmbed += qMetricsEmbed.recall;
            totalApEmbed += qMetricsEmbed.averagePrecision;
            totalMrrEmbed += qMetricsEmbed.reciprocalRank;

            List<RankedItem> topKTfIdf = rankedTfIdf.stream().limit(k)
                    .map(d -> new RankedItem(d.title(), Math.round(d.score() * 1000.0) / 1000.0, d.isRelevant()))
                    .toList();

            List<RankedItem> topKEmbed = rankedEmbed.stream().limit(k)
                    .map(d -> new RankedItem(d.title(), Math.round(d.score() * 1000.0) / 1000.0, d.isRelevant()))
                    .toList();

            List<String> relevantTitles = relevantIndices.stream().map(i -> corpus.get(i).title()).toList();

            breakdown.add(new QueryEvaluationDetail(
                    qPaper.title(),
                    relevantTitles,
                    topKTfIdf,
                    topKEmbed,
                    Math.round(qMetricsTfIdf.averagePrecision * 1000.0) / 1000.0,
                    Math.round(qMetricsEmbed.averagePrecision * 1000.0) / 1000.0
            ));
        }

        int numQueries = queries.size();
        double pTfIdf = round(totalPrecisionTfIdf / numQueries);
        double rTfIdf = round(totalRecallTfIdf / numQueries);
        double f1TfIdf = round(computeF1(pTfIdf, rTfIdf));
        double mapTfIdf = round(totalApTfIdf / numQueries);
        double mrrTfIdf = round(totalMrrTfIdf / numQueries);

        double pEmbed = round(totalPrecisionEmbed / numQueries);
        double rEmbed = round(totalRecallEmbed / numQueries);
        double f1Embed = round(computeF1(pEmbed, rEmbed));
        double mapEmbed = round(totalApEmbed / numQueries);
        double mrrEmbed = round(totalMrrEmbed / numQueries);

        MethodMetrics tfIdfSummary = new MethodMetrics(pTfIdf, rTfIdf, f1TfIdf, mapTfIdf, mrrTfIdf, tfIdfTime);
        MethodMetrics embedSummary = new MethodMetrics(pEmbed, rEmbed, f1Embed, mapEmbed, mrrEmbed, embedTime);

        String conclusion = String.format(
                "Evaluation across %d queries demonstrates that Dense Semantic Embeddings achieved an MRR of %.3f and MAP of %.3f, " +
                "compared with TF-IDF baseline of MRR %.3f and MAP %.3f. Dense vectors capture subword morphological overlap and semantic " +
                "co-occurrence across vocabulary variations, while TF-IDF excels when query terms feature exact keyword specificity.",
                numQueries, mrrEmbed, mapEmbed, mrrTfIdf, mapTfIdf
        );

        return new ExperimentResultResponse(
                "How effectively can semantic embedding techniques identify related academic papers compared with traditional TF-IDF-based similarity?",
                "Curated academic benchmark spanning NLP (Transformers/BERT), Computer Vision (ResNet), Reinforcement Learning (DQN), and Graph Neural Networks (GCN).",
                corpus.size(),
                numQueries,
                k,
                tfIdfSummary,
                embedSummary,
                breakdown,
                conclusion
        );
    }

    private Metrics computeMetricsForQuery(List<ScoredDoc> ranked, int totalRelevant, int k) {
        int relevantRetrieved = 0;
        double sumPrecision = 0.0;
        double reciprocalRank = 0.0;

        for (int i = 0; i < Math.min(k, ranked.size()); i++) {
            if (ranked.get(i).isRelevant()) {
                relevantRetrieved++;
                sumPrecision += ((double) relevantRetrieved / (i + 1));
                if (reciprocalRank == 0.0) {
                    reciprocalRank = 1.0 / (i + 1);
                }
            }
        }

        // Search deeper for reciprocal rank if not in top k
        if (reciprocalRank == 0.0) {
            for (int i = k; i < ranked.size(); i++) {
                if (ranked.get(i).isRelevant()) {
                    reciprocalRank = 1.0 / (i + 1);
                    break;
                }
            }
        }

        double precision = (double) relevantRetrieved / k;
        double recall = totalRelevant > 0 ? ((double) relevantRetrieved / totalRelevant) : 0.0;
        double averagePrecision = totalRelevant > 0 ? (sumPrecision / totalRelevant) : 0.0;

        return new Metrics(precision, recall, averagePrecision, reciprocalRank);
    }

    private double computeF1(double precision, double recall) {
        if (precision + recall == 0.0) return 0.0;
        return (2.0 * precision * recall) / (precision + recall);
    }

    private double round(double val) {
        return Math.round(val * 1000.0) / 1000.0;
    }

    private List<BenchmarkPaper> loadBenchmarkCorpus() {
        return List.of(
                new BenchmarkPaper("Attention Is All You Need",
                        "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks. We propose the Transformer, a model architecture eschewing recurrence and relying entirely on an attention mechanism to draw global dependencies between input and output.", "NLP"),
                new BenchmarkPaper("BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding",
                        "We introduce a new language representation model called BERT, which stands for Bidirectional Encoder Representations from Transformers. Unlike recent language representation models, BERT is designed to pre-train deep bidirectional representations from unlabeled text.", "NLP"),
                new BenchmarkPaper("RoBERTa: A Robustly Optimized BERT Pretraining Approach",
                        "Language model pretraining has led to significant performance gains, but careful comparisons between approaches are challenging. We present a replication study of BERT pretraining that carefully measures the impact of many key hyperparameters and training data size.", "NLP"),
                new BenchmarkPaper("Deep Residual Learning for Image Recognition",
                        "Deeper neural networks are more difficult to train. We present a residual learning framework to ease the training of networks that are substantially deeper than those used previously. We explicitly reformulate the layers as learning residual functions with reference to the layer inputs.", "Vision"),
                new BenchmarkPaper("Identity Mappings in Deep Residual Networks",
                        "Deep residual networks have emerged as a family of extremely deep architectures showing compelling accuracy and nice convergence behaviors. In this paper, we analyze the propagation formulations behind the residual building blocks.", "Vision"),
                new BenchmarkPaper("Playing Atari with Deep Reinforcement Learning",
                        "We present the first deep learning model to successfully learn control policies directly from high-dimensional sensory input using reinforcement learning. The model is a convolutional neural network, trained with a variant of Q-learning, whose input is raw pixels.", "RL"),
                new BenchmarkPaper("Human-level Control Through Deep Reinforcement Learning",
                        "The theory of reinforcement learning provides a normative account deeply rooted in psychological and neuroscientific perspectives on how agents optimize control in complex environments. We demonstrate Deep Q-Networks solving challenging tasks.", "RL"),
                new BenchmarkPaper("Semi-Supervised Classification with Graph Convolutional Networks",
                        "We present a scalable approach for semi-supervised learning on graph-structured data that is based on an efficient variant of convolutional neural networks which operate directly on graphs. We motivate our convolutional architecture via a localized first-order approximation.", "GNN")
        );
    }

    private List<BenchmarkQuery> loadBenchmarkQueries() {
        return List.of(
                new BenchmarkQuery(0, Set.of(1, 2)), // Transformer query -> BERT, RoBERTa are relevant
                new BenchmarkQuery(3, Set.of(4)),    // ResNet query -> Identity Mappings is relevant
                new BenchmarkQuery(5, Set.of(6))     // Atari DQN query -> Deep Q-Networks is relevant
        );
    }

    private record BenchmarkPaper(String title, String abstractText, String domain) {}
    private record BenchmarkQuery(int corpusIndex, Set<Integer> relevantIndices) {}
    private record ScoredDoc(int index, String title, double score, boolean isRelevant) {}
    private record Metrics(double precision, double recall, double averagePrecision, double reciprocalRank) {}
}
