package com.example.cloudhospital.doctor;

import com.example.cloudhospital.common.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/doctors")
public class DoctorController {
    private final DoctorRepository doctors;
    public DoctorController(DoctorRepository doctors) { this.doctors = doctors; }
    @GetMapping public ApiResponse<List<Doctor>> list(@RequestParam(required=false) String departmentName, @RequestParam(required=false) Boolean enabled) {
        if (departmentName != null && enabled != null) return ApiResponse.ok(doctors.findByDepartmentNameAndEnabledOrderByRealNameAsc(departmentName, enabled));
        if (departmentName != null) return ApiResponse.ok(doctors.findByDepartmentNameOrderByRealNameAsc(departmentName));
        return ApiResponse.ok(enabled == null ? doctors.findAll() : doctors.findByEnabledOrderByDepartmentNameAscRealNameAsc(enabled));
    }
    @GetMapping("/{id}") public ApiResponse<Doctor> detail(@PathVariable Long id) { return ApiResponse.ok(doctors.findById(id).orElseThrow(() -> new BizException(40002, "医生不存在"))); }
}
