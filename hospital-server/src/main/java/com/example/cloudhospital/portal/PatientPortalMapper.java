package com.example.cloudhospital.portal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface PatientPortalMapper {
  List<Map<String, Object>> findEnabledDoctors();

  Long countQueueAhead(@Param("doctorId") Long doctorId, @Param("visitDate") LocalDate visitDate);

  Map<String, Object> findPatientOverview(@Param("patientId") Long patientId);

  List<Map<String, Object>> findRegistrationsByPatientId(@Param("patientId") Long patientId);

  List<Map<String, Object>> findPrescriptionsByPatientId(@Param("patientId") Long patientId);

  List<Map<String, Object>> findNotificationsByPatientId(@Param("patientId") Long patientId);

  int markNotificationsReadByPatientId(@Param("patientId") Long patientId);
}
