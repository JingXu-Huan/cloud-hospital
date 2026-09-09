package com.example.cloudhospital.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardMapper {
    long countPatients();
    long countEnabledDoctors();
    List<DashboardController.CountByStatus> countRegistrationsByStatus(@Param("visitDate") LocalDate visitDate);
    List<DashboardController.CountByStatus> countPrescriptionsByStatus();
    BigDecimal sumUnpaidAmount();
}
