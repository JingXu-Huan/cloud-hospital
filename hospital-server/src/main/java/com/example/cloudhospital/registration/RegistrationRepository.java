package com.example.cloudhospital.registration;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper
public interface RegistrationRepository {
  Optional<Registration> findById(Long id);

  int save(Registration registration);

  boolean existsByPatientIdAndDoctorIdAndVisitDateAndStatusIn(
      @Param("patientId") Long patientId,
      @Param("doctorId") Long doctorId,
      @Param("visitDate") LocalDate visitDate,
      @Param("statuses") Collection<RegistrationStatus> statuses);

  List<Registration> findByDoctorIdAndVisitDateAndStatusInOrderByRegisteredAtAsc(
      @Param("doctorId") Long doctorId,
      @Param("visitDate") LocalDate visitDate,
      @Param("statuses") Collection<RegistrationStatus> statuses);

  int startIfWaiting(
      @Param("id") Long id,
      @Param("now") LocalDateTime now,
      @Param("expectedStatus") RegistrationStatus expectedStatus,
      @Param("nextStatus") RegistrationStatus nextStatus);

  int cancelIfWaiting(
      @Param("id") Long id,
      @Param("reason") String reason,
      @Param("expectedStatus") RegistrationStatus expectedStatus,
      @Param("nextStatus") RegistrationStatus nextStatus);

  int completeIfInProgress(
      @Param("id") Long id,
      @Param("now") LocalDateTime now,
      @Param("expectedStatus") RegistrationStatus expectedStatus,
      @Param("nextStatus") RegistrationStatus nextStatus);

  int deleteIfWaiting(@Param("id") Long id, @Param("expectedStatus") RegistrationStatus expectedStatus);
}
