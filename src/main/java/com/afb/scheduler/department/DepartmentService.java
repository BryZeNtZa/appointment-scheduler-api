package com.afb.scheduler.department;

import com.afb.scheduler.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Department> list() {
        return departmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Department getByCode(String code) {
        return departmentRepository.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", code));
    }
}
