package com.example.cloudhospital.prescription;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PrescriptionNotificationMapper {
    int countPatientAccounts(@Param("patientId") Long patientId);
    int insertPickupReadyNotification(@Param("patientId") Long patientId, @Param("prescriptionId") Long prescriptionId, @Param("prescriptionNo") String prescriptionNo);
    int markNotificationsReadByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
