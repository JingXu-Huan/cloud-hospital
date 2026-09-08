package com.example.cloudhospital.medicalrecord;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface MedicalRecordRepository {
    Optional<MedicalRecord> findByRegistrationId(Long registrationId);
    int save(MedicalRecord record);
}
