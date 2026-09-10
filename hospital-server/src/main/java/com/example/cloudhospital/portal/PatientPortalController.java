package com.example.cloudhospital.portal;

import com.example.cloudhospital.auth.AuthService;
import com.example.cloudhospital.common.ApiResponse;
import com.example.cloudhospital.common.BizException;
import com.example.cloudhospital.prescription.Prescription;
import com.example.cloudhospital.prescription.PrescriptionService;
import com.example.cloudhospital.registration.Registration;
import com.example.cloudhospital.registration.RegistrationController;
import com.example.cloudhospital.registration.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/patient-portal")
public class PatientPortalController {
  private final PatientPortalMapper patientPortalMapper;
  private final RegistrationService registrations;
  private final PrescriptionService prescriptions;

  public PatientPortalController(
      PatientPortalMapper patientPortalMapper,
      RegistrationService registrations,
      PrescriptionService prescriptions) {
    this.patientPortalMapper = patientPortalMapper;
    this.registrations = registrations;
    this.prescriptions = prescriptions;
  }

  @GetMapping("/doctors")
  public ApiResponse<List<Map<String, Object>>> doctors() {
    return ApiResponse.ok(patientPortalMapper.findEnabledDoctors());
  }

  @GetMapping("/queue-preview")
  public ApiResponse<QueuePreview> queuePreview(
      @RequestParam Long doctorId, @RequestParam LocalDate visitDate) {
    Long ahead = patientPortalMapper.countQueueAhead(doctorId, visitDate);
    return ApiResponse.ok(new QueuePreview(ahead == null ? 0 : ahead));
  }

  @PostMapping("/registrations")
  public ApiResponse<RegistrationController.RegistrationView> register(
      @Valid @RequestBody Register r, HttpServletRequest req) {
    Long id = session(req).patientId();
    Registration x = registrations.create(id, r.doctorId(), r.visitDate());
    return ApiResponse.ok(
        new RegistrationController.RegistrationView(
            x.id,
            x.visitNo,
            x.patientId,
            null,
            x.doctorId,
            null,
            x.visitDate,
            x.registrationFee,
            x.status,
            x.registeredAt,
            x.consultationStartedAt,
            x.consultationEndedAt,
            x.cancelReason));
  }

  @PostMapping("/registrations/{id}/cancel")
  public ApiResponse<Void> cancelRegistration(
      @PathVariable Long id,
      @RequestBody(required = false) RegistrationController.CancelRequest request,
      HttpServletRequest req) {
    Registration registration = registrations.get(id);
    if (!Objects.equals(registration.patientId, session(req).patientId()))
      throw new BizException(40300, "无权取消该挂号");
    registrations.cancel(id, request == null ? "患者自助取消" : request.reason());
    return ApiResponse.ok();
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

  @GetMapping("/prescriptions/{id}/items")
  public ApiResponse<List<com.example.cloudhospital.prescription.PrescriptionItem>>
      prescriptionItems(@PathVariable Long id, HttpServletRequest req) {
    Prescription prescription = prescriptions.get(id);
    if (!Objects.equals(prescription.patientId, session(req).patientId()))
      throw new BizException(40300, "无权查看该处方明细");
    return ApiResponse.ok(prescriptions.items(id));
  }

  @PostMapping("/notifications/read")
  public ApiResponse<Void> markNotificationsRead(HttpServletRequest req) {
    patientPortalMapper.markNotificationsReadByPatientId(session(req).patientId());
    return ApiResponse.ok();
  }

  @GetMapping("/overview")
  public ApiResponse<Map<String, Object>> overview(HttpServletRequest req) {
    Long id = session(req).patientId();
    Map<String, Object> p = patientPortalMapper.findPatientOverview(id);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("patient", p);
    out.put("registrations", patientPortalMapper.findRegistrationsByPatientId(id));
    out.put("prescriptions", patientPortalMapper.findPrescriptionsByPatientId(id));
    out.put("notifications", patientPortalMapper.findNotificationsByPatientId(id));
    return ApiResponse.ok(out);
  }

  private AuthService.Session session(HttpServletRequest req) {
    return (AuthService.Session) req.getAttribute("session");
  }

  public record Register(@NotNull Long doctorId, @NotNull LocalDate visitDate) {}

  public record QueuePreview(long aheadCount) {}
}
