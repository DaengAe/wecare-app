package com.example.wecare.routine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoutineMemoRequest {
    private String guardianMemo;
    private String dependentMemo;
}
