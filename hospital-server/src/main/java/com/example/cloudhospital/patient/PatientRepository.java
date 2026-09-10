package com.example.cloudhospital.patient;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PatientRepository {
  Optional<Patient> findById(Long id);

  Optional<Patient> findByIdCard(String idCard);

  List<Patient> findTop20ByNameContainingOrPatientNoContainingOrderByCreatedAtDesc(
      String name, String patientNo);

  boolean existsRegistrationByPatientId(Long patientId);

  int save(Patient patient);

  int deleteById(Long id);
}
