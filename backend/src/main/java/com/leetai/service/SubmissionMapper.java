package com.leetai.service;

import com.leetai.dto.SubmissionRecordResponse;
import com.leetai.model.Submission;
import org.springframework.stereotype.Component;

@Component
public class SubmissionMapper {

    public SubmissionRecordResponse toResponse(Submission s) {
        SubmissionRecordResponse res = new SubmissionRecordResponse();
        res.setId(s.getId());
        res.setProblemSlug(s.getProblem().getSlug());
        res.setProblemName(s.getProblem().getName());
        res.setUserEmail(s.getUser().getEmail());
        res.setUserName(s.getUser().getName());
        res.setLanguage(s.getLanguage());
        res.setCode(s.getCode());
        res.setApproach(s.getApproach());
        res.setPassedCount(s.getPassedCount());
        res.setTotalCount(s.getTotalCount());
        res.setAllPassed(s.isAllPassed());
        res.setSolved(s.isSolved());
        res.setMarkedBy(s.getMarkedBy());
        res.setAiFeedback(s.getAiFeedback());
        res.setCreatedAt(s.getCreatedAt());
        return res;
    }
}
