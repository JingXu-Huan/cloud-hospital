package com.example.cloudhospital.prescription;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface PrescriptionRepository {
    Optional<Prescription> findById(Long id);
    List<Prescription> findAll();
    int save(Prescription prescription);
    List<Prescription> findByStatusOrderByPrescribedAtAsc(PrescriptionStatus status);
    List<Prescription> findByRegistrationId(Long registrationId);
    int payIfUnpaid(@Param("id") Long id, @Param("method") String method, @Param("now") LocalDateTime now, @Param("expectedStatus") PrescriptionStatus expectedStatus, @Param("nextStatus") PrescriptionStatus nextStatus);
    int dispenseIfPaid(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("expectedStatus") PrescriptionStatus expectedStatus, @Param("nextStatus") PrescriptionStatus nextStatus);
    int pickUpIfDispensed(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("expectedStatus") PrescriptionStatus expectedStatus, @Param("nextStatus") PrescriptionStatus nextStatus);
    int cancelIfUnpaid(@Param("id") Long id, @Param("expectedStatus") PrescriptionStatus expectedStatus, @Param("nextStatus") PrescriptionStatus nextStatus);
}
