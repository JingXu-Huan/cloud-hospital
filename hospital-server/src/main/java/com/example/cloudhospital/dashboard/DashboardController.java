package com.example.cloudhospital.dashboard;

import com.example.cloudhospital.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  private final DashboardMapper dashboardMapper;

  public DashboardController(DashboardMapper dashboardMapper) {
    this.dashboardMapper = dashboardMapper;
  }

  @GetMapping("/overview")
  public ApiResponse<Overview> overview() {
    LocalDate today = LocalDate.now();
    long patients = dashboardMapper.countPatients();
    long doctors = dashboardMapper.countEnabledDoctors();
    List<CountByStatus> registrations = dashboardMapper.countRegistrationsByStatus(today);
    List<CountByStatus> prescriptions = dashboardMapper.countPrescriptionsByStatus();
    BigDecimal unpaidAmount = dashboardMapper.sumUnpaidAmount();
    return ApiResponse.ok(
        new Overview(patients, doctors, registrations, prescriptions, unpaidAmount));
  }

  public record Overview(
      long patientCount,
      long enabledDoctorCount,
      List<CountByStatus> todayRegistrations,
      List<CountByStatus> prescriptions,
      BigDecimal unpaidAmount) {}

  public record CountByStatus(String status, long total) {}
}
