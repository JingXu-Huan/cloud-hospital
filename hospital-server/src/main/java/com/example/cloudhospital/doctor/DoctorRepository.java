package com.example.cloudhospital.doctor;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DoctorRepository {
  Optional<Doctor> findById(Long id);

  Optional<Doctor> findByLoginName(String loginName);

  List<Doctor> findAll();

  List<Doctor> findByEnabledOrderByDepartmentNameAscRealNameAsc(Boolean enabled);

  List<Doctor> findByDepartmentNameAndEnabledOrderByRealNameAsc(
      String departmentName, Boolean enabled);

  List<Doctor> findByDepartmentNameOrderByRealNameAsc(String departmentName);

  boolean existsRegistrationByDoctorId(Long doctorId);

  int save(Doctor doctor);

  int deleteById(Long id);
}
