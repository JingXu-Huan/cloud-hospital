package com.example.cloudhospital.portal;

import com.example.cloudhospital.common.*;
import com.example.cloudhospital.registration.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.example.cloudhospital.auth.AuthService;
import com.example.cloudhospital.prescription.Prescription;
import com.example.cloudhospital.prescription.PrescriptionService;

@RestController
@RequestMapping("/api/v1/patient-portal")
public class PatientPortalController {
    private final JdbcTemplate jdbc;
    private final RegistrationService registrations;
    private final PrescriptionService prescriptions;

    public PatientPortalController(JdbcTemplate jdbc, RegistrationService registrations, PrescriptionService prescriptions) {
        this.jdbc = jdbc;
        this.registrations = registrations;
        this.prescriptions = prescriptions;
    }

    @GetMapping("/doctors")
    public ApiResponse<List<Map<String, Object>>> doctors() {
        return ApiResponse.ok(jdbc.queryForList("SELECT id, doctor_no AS doctorNo, real_name AS realName, department_name AS departmentName, title FROM doctor WHERE enabled=b'1' ORDER BY department_name,real_name"));
    }

    @GetMapping("/queue-preview")
    public ApiResponse<QueuePreview> queuePreview(@RequestParam Long doctorId, @RequestParam LocalDate visitDate) {
        Long ahead = jdbc.queryForObject("SELECT COUNT(*) FROM registration WHERE doctor_id=? AND visit_date=? AND status IN ('WAITING','IN_PROGRESS')", Long.class, doctorId, visitDate);
        return ApiResponse.ok(new QueuePreview(ahead == null ? 0 : ahead));
    }

    @PostMapping("/registrations")
    public ApiResponse<RegistrationController.RegistrationView> register(@Valid @RequestBody Register r, HttpServletRequest req) {
        Long id = session(req).patientId();
        Registration x = registrations.create(id, r.doctorId(), r.visitDate());
        return ApiResponse.ok(new RegistrationController.RegistrationView(x.id, x.visitNo, x.patientId, null, x.doctorId, null, x.visitDate, x.registrationFee, x.status, x.registeredAt, x.consultationStartedAt, x.consultationEndedAt, x.cancelReason));
    }

    @PostMapping("/prescriptions/{id}/pay")
    public ApiResponse<Void> pay(@PathVariable Long id, HttpServletRequest req) {
        Prescription prescription = prescriptions.get(id);
        if (!Objects.equals(prescription.patientId, session(req).patientId()))
            throw new BizException(40300, "无权支付该收费订单");
        prescriptions.pay(id, "WECHAT");
        return ApiResponse.ok();
    }

    @PostMapping("/prescriptions/{id}/pickup")
    public ApiResponse<Void> pickUp(@PathVariable Long id, HttpServletRequest req) {
        Prescription prescription = prescriptions.get(id);
        if (!Objects.equals(prescription.patientId, session(req).patientId()))
            throw new BizException(40300, "无权确认该处方取药");
        prescriptions.pickUp(id);
        return ApiResponse.ok();
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(HttpServletRequest req) {
        Long id = session(req).patientId();
        Map<String, Object> p = jdbc.queryForMap("SELECT id,patient_no patientNo,name,gender,phone FROM patient WHERE id=?", id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("patient", p);
        out.put("registrations", jdbc.queryForList("SELECT r.visit_no visitNo,r.visit_date visitDate,r.status,d.real_name doctorName,d.department_name departmentName, CASE WHEN r.status='WAITING' THEN (SELECT COUNT(*) FROM registration q WHERE q.doctor_id=r.doctor_id AND q.visit_date=r.visit_date AND q.status IN ('WAITING','IN_PROGRESS') AND (q.registered_at<r.registered_at OR (q.registered_at=r.registered_at AND q.id<r.id))) ELSE 0 END queueAhead FROM registration r JOIN doctor d ON d.id=r.doctor_id WHERE r.patient_id=? ORDER BY r.registered_at DESC", id));
        out.put("prescriptions", jdbc.queryForList("SELECT id,prescription_no prescriptionNo,status,total_amount totalAmount FROM prescription WHERE patient_id=? ORDER BY prescribed_at DESC", id));
        out.put("notifications", jdbc.queryForList("SELECT id,type,title,content,created_at createdAt,read_at readAt FROM patient_notification WHERE patient_id=? ORDER BY created_at DESC", id));
        return ApiResponse.ok(out);
    }

    private AuthService.Session session(HttpServletRequest req) {
        return (AuthService.Session) req.getAttribute("session");
    }

    public record Register(@NotNull Long doctorId, @NotNull LocalDate visitDate) {
    }

    public record QueuePreview(long aheadCount) {
    }
}
