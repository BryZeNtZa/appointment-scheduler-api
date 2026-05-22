package com.afb.scheduler.department.dto;

import com.afb.scheduler.department.Department;

public record DepartmentResponse(
        String code,
        String label
) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getCode(), department.getLabel());
    }
}
