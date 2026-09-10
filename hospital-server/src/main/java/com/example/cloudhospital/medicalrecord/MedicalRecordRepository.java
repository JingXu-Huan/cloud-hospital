package com.example.cloudhospital.medicalrecord;

import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface MedicalRecordRepository {
  Optional<MedicalRecord> findByRegistrationId(Long registrationId);

  int save(MedicalRecord record);

  int deleteByRegistrationId(Long registrationId);
}
