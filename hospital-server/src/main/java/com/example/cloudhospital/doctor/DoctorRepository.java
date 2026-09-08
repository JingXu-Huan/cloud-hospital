package com.example.cloudhospital.doctor;

import java.util.*;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorRepository {
    Optional<Doctor> findById(Long id);
    Optional<Doctor> findByLoginName(String loginName);
    List<Doctor> findAll();
    List<Doctor> findByEnabledOrderByDepartmentNameAscRealNameAsc(Boolean enabled);
    List<Doctor> findByDepartmentNameAndEnabledOrderByRealNameAsc(String departmentName, Boolean enabled);
    List<Doctor> findByDepartmentNameOrderByRealNameAsc(String departmentName);
    int save(Doctor doctor);
}
