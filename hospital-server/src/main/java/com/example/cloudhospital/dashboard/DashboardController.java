package com.example.cloudhospital.dashboard;

import com.example.cloudhospital.common.ApiResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final JdbcTemplate jdbc;
    public DashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/overview")
    public ApiResponse<Overview> overview() {
        LocalDate today = LocalDate.now();
        long patients = jdbc.queryForObject("SELECT COUNT(*) FROM patient", Long.class);
        long doctors = jdbc.queryForObject("SELECT COUNT(*) FROM doctor WHERE enabled = b'1'", Long.class);
        List<CountByStatus> registrations = jdbc.query("SELECT status, COUNT(*) total FROM registration WHERE visit_date = ? GROUP BY status", (rs, n) -> new CountByStatus(rs.getString("status"), rs.getLong("total")), today);
        List<CountByStatus> prescriptions = jdbc.query("SELECT status, COUNT(*) total FROM prescription GROUP BY status", (rs, n) -> new CountByStatus(rs.getString("status"), rs.getLong("total")));
        BigDecimal unpaidAmount = jdbc.queryForObject("SELECT COALESCE(SUM(total_amount), 0) FROM prescription WHERE status = 'UNPAID'", BigDecimal.class);
        return ApiResponse.ok(new Overview(patients, doctors, registrations, prescriptions, unpaidAmount));
    }
    public record Overview(long patientCount, long enabledDoctorCount, List<CountByStatus> todayRegistrations, List<CountByStatus> prescriptions, BigDecimal unpaidAmount) {}
    public record CountByStatus(String status, long total) {}
}
