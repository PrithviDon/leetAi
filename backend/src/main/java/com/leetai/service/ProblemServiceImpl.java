package com.leetai.service;

import com.leetai.dto.CreateProblemRequest;
import com.leetai.dto.ProblemResponse;
import com.leetai.model.Problem;
import com.leetai.repository.ProblemRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemMapper problemMapper;
    private final SlugGenerator slugGenerator;
    private final ElasticsearchSyncService esSyncService;

    public ProblemServiceImpl(ProblemRepository problemRepository,
                               ProblemMapper problemMapper,
                               SlugGenerator slugGenerator,
                               ElasticsearchSyncService esSyncService) {
        this.problemRepository = problemRepository;
        this.problemMapper = problemMapper;
        this.slugGenerator = slugGenerator;
        this.esSyncService = esSyncService;
    }

    @Override
    public List<ProblemResponse> listPublished() {
        return problemRepository.findAll().stream()
                .filter(p -> p.getStatus() == Problem.Status.PUBLISHED)
                .map(p -> problemMapper.toResponse(p, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProblemResponse> listAllForAdmin() {
        return problemRepository.findAll().stream()
                .map(p -> problemMapper.toResponse(p, false))
                .collect(Collectors.toList());
    }

    @Override
    public ProblemResponse getPublishedBySlug(String slug) {
        Problem problem = getEntityBySlug(slug);
        if (problem.getStatus() != Problem.Status.PUBLISHED) {
            throw new RuntimeException("Problem not found: " + slug);
        }
        return problemMapper.toResponse(problem, false);
    }

    @Override
    public ProblemResponse getForAdminBySlug(String slug) {
        return problemMapper.toResponse(getEntityBySlug(slug), true);
    }

    @Override
    public Problem getEntityBySlug(String slug) {
        return problemRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + slug));
    }

    @Override
    public ProblemResponse create(CreateProblemRequest request, String adminEmail) {
        String slug = slugGenerator.generate(request.getName());
        Problem problem = problemMapper.toEntity(request, slug);
        problem.setStatus(Problem.Status.DRAFT);
        problem.setCreatedBy(adminEmail);
        problem.setUpdatedBy(adminEmail);
        Problem saved = problemRepository.save(problem);
        // Draft — sync to ES so admins can search it too, but status=DRAFT
        // means it won't appear in the public search results.
        esSyncService.index(saved);
        return problemMapper.toResponse(saved, true);
    }

    @Override
    public ProblemResponse update(String slug, CreateProblemRequest request, String adminEmail) {
        Problem existing = getEntityBySlug(slug);
        problemMapper.applyUpdate(existing, request);
        existing.setUpdatedBy(adminEmail);
        existing.setUpdatedAt(Instant.now());
        Problem saved = problemRepository.save(existing);
        esSyncService.index(saved); // keep name/description/difficulty in sync
        return problemMapper.toResponse(saved, true);
    }

    @Override
    public void delete(String slug) {
        Problem existing = getEntityBySlug(slug);
        Long id = existing.getId();
        problemRepository.delete(existing);
        esSyncService.remove(id); // remove from search index too
    }

    @Override
    public ProblemResponse publish(String slug, String adminEmail) {
        Problem problem = getEntityBySlug(slug);
        problem.setStatus(Problem.Status.PUBLISHED);
        problem.setUpdatedBy(adminEmail);
        problem.setUpdatedAt(Instant.now());
        Problem saved = problemRepository.save(problem);
        esSyncService.index(saved); // status=PUBLISHED now — appears in public search
        return problemMapper.toResponse(saved, true);
    }

    @Override
    public ProblemResponse unpublish(String slug, String adminEmail) {
        Problem problem = getEntityBySlug(slug);
        problem.setStatus(Problem.Status.DRAFT);
        problem.setUpdatedBy(adminEmail);
        problem.setUpdatedAt(Instant.now());
        Problem saved = problemRepository.save(problem);
        esSyncService.index(saved); // status=DRAFT — filtered out of public search
        return problemMapper.toResponse(saved, true);
    }
}
