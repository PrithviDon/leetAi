package com.leetai.repository;

import com.leetai.document.ProblemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProblemEsRepository extends ElasticsearchRepository<ProblemDocument, String> {
    // Custom queries are in ElasticsearchSearchService using ElasticsearchOperations
    // for full control over the query DSL — Spring Data's derived method names
    // don't support multi_match + filter combinations cleanly.
}
