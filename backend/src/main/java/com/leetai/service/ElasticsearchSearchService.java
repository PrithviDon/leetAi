    package com.leetai.service;

    import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
    import co.elastic.clients.elasticsearch._types.query_dsl.Query;
    import com.leetai.document.ProblemDocument;
    import com.leetai.dto.ProblemSearchResult;
    import org.springframework.data.domain.PageImpl;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.elasticsearch.client.elc.NativeQuery;
    import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
    import org.springframework.data.elasticsearch.core.SearchHits;
    import org.springframework.stereotype.Service;

    import java.util.ArrayList;
    import java.util.List;

    @Service
    public class ElasticsearchSearchService {

        private final ElasticsearchOperations esOps;

        public ElasticsearchSearchService(ElasticsearchOperations esOps) {
            this.esOps = esOps;
        }

        public PageImpl<ProblemSearchResult> search(String searchTerm,
                                                    String difficulty,
                                                    Pageable pageable) {

            List<Query> filters = new ArrayList<>();

            // Restrict to PUBLISHED status
            filters.add(Query.of(q -> q.term(t -> t.field("status").value("PUBLISHED"))));

            // Optional difficulty filter
            if (difficulty != null && !difficulty.isBlank() && !difficulty.equalsIgnoreCase("ALL")) {
                String upper = difficulty.toUpperCase();
                filters.add(Query.of(q -> q.term(t -> t.field("difficulty").value(upper))));
            }

            // Relevance query
            Query mainQuery;
            if (searchTerm != null && !searchTerm.isBlank()) {
                mainQuery = Query.of(q -> q.multiMatch(m -> m
                        .query(searchTerm)
                        .fields(List.of("name^3", "description"))
                        .fuzziness("AUTO")
                        .minimumShouldMatch("75%")
                ));
            } else {
                mainQuery = Query.of(q -> q.matchAll(m -> m));
            }

            Query finalMainQuery = mainQuery;
            List<Query> finalFilters = filters;

            Query boolQuery = Query.of(q -> q.bool(b -> {
                BoolQuery.Builder builder = b.must(finalMainQuery);
                for (Query f : finalFilters) {
                    builder = builder.filter(f);
                }
                return builder;
            }));

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(boolQuery)
                    .withPageable(pageable)
                    .build();

            SearchHits<ProblemDocument> hits = esOps.search(nativeQuery, ProblemDocument.class);

            List<ProblemSearchResult> results = hits.getSearchHits().stream()
                    .map(hit -> {
                        ProblemDocument doc = hit.getContent();
                        return new ProblemSearchResult(
                                doc.getSlug(),
                                doc.getName(),
                                doc.getDifficulty()
                        );
                    })
                    .toList();

            return new PageImpl<>(results, pageable, hits.getTotalHits());
        }
    }