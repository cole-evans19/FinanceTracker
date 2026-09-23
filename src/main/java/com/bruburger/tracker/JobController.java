package com.bruburger.tracker;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobDAO jobDAO;

    public JobController(JobDAO jobDAO) {
        this.jobDAO = jobDAO;
    }

    public record CreateJobRequest(String name) {}

    @GetMapping
    public List<Job> getJobs(Authentication authentication) {
        int userId = SecurityUtils.getUserId(authentication);
        return jobDAO.findJobsByUser(userId);
    }

    @PostMapping
    public Job createJob(Authentication authentication, @RequestBody CreateJobRequest request) {
        int userId = SecurityUtils.getUserId(authentication);
        return jobDAO.insertJob(userId, request.name());
    }

    @DeleteMapping("/{id}")
    public void deleteJob(Authentication authentication, @PathVariable int id) {
        int userId = SecurityUtils.getUserId(authentication);
        jobDAO.deleteJob(userId, id);
    }
}