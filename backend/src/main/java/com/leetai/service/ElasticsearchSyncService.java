package com.leetai.service;

import com.leetai.document.ProblemDocument;
import com.leetai.model.Problem;
import com.leetai.repository.ProblemEsRepository;
import com.leetai.repository.ProblemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElasticsearchSyncService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchSyncService.class);

    private final ProblemEsRepository esRepository;
    private final ProblemRepository sqlRepository;

    public ElasticsearchSyncService(ProblemEsRepository esRepository, ProblemRepository sqlRepository) {
        this.esRepository = esRepository;
        this.sqlRepository = sqlRepository;
    }

    /**
     * Index or update a single problem.
     */
    public void index(Problem problem) {
        try {
            ProblemDocument doc = toDocument(problem);
            esRepository.save(doc);
            log.debug("Indexed problem {} in Elasticsearch", problem.getSlug());
        } catch (Exception e) {
            log.error("Failed to index problem {} in Elasticsearch", problem.getSlug(), e);
        }
    }

    /**
     * Remove a problem by MySQL ID.
     */
    public void remove(Long problemId) {
        try {
            esRepository.deleteById(String.valueOf(problemId));
            log.debug("Removed problem {} from Elasticsearch", problemId);
        } catch (Exception e) {
            log.error("Failed to remove problem {} from Elasticsearch", problemId, e);
        }
    }

    /**
     * Bulk re-indexes all rows from MySQL to Elasticsearch.
     */
    public int reindexAll() {
        log.info("Starting bulk synchronization from MySQL to Elasticsearch...");
        List<Problem> problems = sqlRepository.findAll();

        if (problems.isEmpty()) {
            log.info("No problems found in MySQL to sync.");
            return 0;
        }

        List<ProblemDocument> docs = problems.stream()
                .map(this::toDocument)
                .toList();

        esRepository.saveAll(docs);
        log.info("Successfully bulk-indexed {} problems into Elasticsearch.", docs.size());
        return docs.size();
    }

    /**
     * Runs automatically when the application starts up.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        try {
            reindexAll();
        } catch (Exception e) {
            log.error("Error running Elasticsearch startup sync", e);
        }
    }

    private ProblemDocument toDocument(Problem problem) {
        return new ProblemDocument(
                String.valueOf(problem.getId()),
                problem.getName(),
                problem.getDescription(),
                problem.getDifficulty() != null ? problem.getDifficulty().name() : null,
                problem.getStatus() != null ? problem.getStatus().name() : null,
                problem.getSlug()
        );
    }
}