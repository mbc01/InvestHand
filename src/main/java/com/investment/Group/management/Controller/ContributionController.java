package com.investment.Group.management.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.investment.Group.management.Service.ContributionService;
import com.investment.Group.management.dto.ContributionRequest;
import com.investment.Group.management.dto.ContributionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for member contributions.
 */
@RestController
@RequestMapping("/api/contributions")
@Tag(name = "Contributions", description = "Record and query member contributions")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @Operation(summary = "Record a contribution for a member")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{memberId}")
    public ResponseEntity<ContributionResponse> createContribution(
            @PathVariable Long memberId,
            @Valid @RequestBody ContributionRequest request) {

        // The boolean records whether the client actually supplied a month, so
        // the service can fall back to the current month without treating an
        // omitted field as a deliberate (possibly blank) value.
        boolean monthSupplied = request.month() != null && !request.month().isBlank();

        ContributionResponse body = ContributionResponse.from(
                contributionService.createContribution(
                        memberId, request.amount(), request.month(), monthSupplied));

        // 201 Created, with a Location header pointing at the new resource.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);
    }

    @Operation(summary = "List every contribution")
    @GetMapping
    public List<ContributionResponse> getAllContributions() {
        return contributionService.getAllContributions()
                .stream()
                .map(ContributionResponse::from)
                .toList();
    }

    @Operation(summary = "List contributions for one member")
    @GetMapping("/member/{memberId}")
    public List<ContributionResponse> getByMember(@PathVariable Long memberId) {
        return contributionService.getContributionsByMember(memberId)
                .stream()
                .map(ContributionResponse::from)
                .toList();
    }
}