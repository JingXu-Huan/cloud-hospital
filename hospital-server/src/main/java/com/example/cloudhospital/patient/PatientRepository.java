package com.example.cloudhospital.patient;

import java.util.*;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientRepository {
    Optional<Patient> findById(Long id);
    Optional<Patient> findByIdCard(String idCard);
    List<Patient> findTop20ByNameContainingOrPatientNoContainingOrderByCreatedAtDesc(String name, String patientNo);
    int save(Patient patient);
}
