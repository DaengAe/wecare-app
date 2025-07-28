package com.example.wecare.routine.controller;

import com.example.wecare.routine.dto.RoutineRequest;
import com.example.wecare.routine.dto.RoutineResponse;
import com.example.wecare.routine.dto.RoutineMemoRequest;
import com.example.wecare.routine.service.RoutineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @PostMapping("/{dependentId}")
    public ResponseEntity<RoutineResponse> createRoutine(
            @PathVariable Long dependentId,
            @RequestBody RoutineRequest request) {
        RoutineResponse response = routineService.createRoutine(dependentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/guardian")
    public ResponseEntity<List<RoutineResponse>> getRoutinesByGuardian() {
        List<RoutineResponse> responses = routineService.getRoutinesByGuardian();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/dependent")
    public ResponseEntity<List<RoutineResponse>> getRoutinesByDependent() {
        List<RoutineResponse> responses = routineService.getRoutinesByDependent();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{routineId}")
    public ResponseEntity<RoutineResponse> getRoutineById(@PathVariable Long routineId) {
        RoutineResponse response = routineService.getRoutineById(routineId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{routineId}")
    public ResponseEntity<RoutineResponse> updateRoutine(
            @PathVariable Long routineId,
            @RequestBody RoutineRequest request) {
        RoutineResponse response = routineService.updateRoutine(routineId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{routineId}")
    public ResponseEntity<Void> deleteRoutine(@PathVariable Long routineId) {
        routineService.deleteRoutine(routineId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{routineId}/complete")
    public ResponseEntity<Void> completeRoutine(@PathVariable Long routineId) {
        routineService.completeRoutine(routineId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{routineId}/memo")
    public ResponseEntity<RoutineResponse> updateRoutineMemo(
            @PathVariable Long routineId,
            @RequestBody RoutineMemoRequest request) {
        RoutineResponse response = routineService.updateRoutineMemo(routineId, request);
        return ResponseEntity.ok(response);
    }
}