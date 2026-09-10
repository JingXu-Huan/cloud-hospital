package com.example.cloudhospital.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardMapper {
  long countPatients();

  long countEnabledDoctors();

  List<DashboardController.CountByStatus> countRegistrationsByStatus(
      @Param("visitDate") LocalDate visitDate);

  List<DashboardController.CountByStatus> countPrescriptionsByStatus();

  BigDecimal sumUnpaidAmount();
}
