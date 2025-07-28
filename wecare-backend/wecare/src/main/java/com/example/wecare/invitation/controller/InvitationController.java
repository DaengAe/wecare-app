package com.example.wecare.invitation.controller;

import com.example.wecare.invitation.dto.AcceptInvitationRequest;
import com.example.wecare.invitation.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateInvitationCode() {
        String code = invitationService.generateInvitationCode();
        return ResponseEntity.ok(Map.of("invitationCode", code));
    }

    @PostMapping("/accept")
    public ResponseEntity<Void> acceptInvitationCode(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.acceptInvitationCode(request.getInvitationCode(), request.getRelationshipType());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/connections/{targetUserId}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long targetUserId) {
        invitationService.deleteConnection(targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/connections/{targetUserId}/reactivate")
    public ResponseEntity<Void> reactivateConnection(@PathVariable Long targetUserId) {
        invitationService.reactivateConnection(targetUserId);
        return ResponseEntity.ok().build();
    }
}