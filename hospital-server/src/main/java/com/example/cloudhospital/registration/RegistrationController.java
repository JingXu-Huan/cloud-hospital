package com.example.cloudhospital.registration;

import com.example.cloudhospital.common.*;
import com.example.cloudhospital.doctor.*;
import com.example.cloudhospital.patient.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/v1")
public class RegistrationController {
    private final RegistrationService service; private final PatientRepository patients; private final DoctorRepository doctors;
    public RegistrationController(RegistrationService service, PatientRepository patients, DoctorRepository doctors) { this.service=service; this.patients=patients; this.doctors=doctors; }
    @PostMapping("/registrations") public ApiResponse<RegistrationView> create(@Valid @RequestBody CreateRequest req) { return ApiResponse.ok(view(service.create(req.patientId(),req.doctorId(),req.visitDate()))); }
    @GetMapping("/registrations/{id}") public ApiResponse<RegistrationView> detail(@PathVariable Long id) { return ApiResponse.ok(view(service.get(id))); }
    @PostMapping("/registrations/{id}/start") public ApiResponse<Void> start(@PathVariable Long id) { service.start(id); return ApiResponse.ok(); }
    @PostMapping("/registrations/{id}/cancel") public ApiResponse<Void> cancel(@PathVariable Long id,@RequestBody(required=false) CancelRequest req) { service.cancel(id,req == null ? null : req.reason()); return ApiResponse.ok(); }
    @PostMapping("/registrations/{id}/complete") public ApiResponse<Void> complete(@PathVariable Long id) { service.complete(id); return ApiResponse.ok(); }
    @GetMapping("/doctors/{doctorId}/registrations") public ApiResponse<List<RegistrationView>> queue(@PathVariable Long doctorId,@RequestParam(required=false) RegistrationStatus status,@RequestParam(required=false) LocalDate visitDate) { return ApiResponse.ok(service.queue(doctorId,visitDate == null ? LocalDate.now() : visitDate,status).stream().map(this::view).toList()); }
    private RegistrationView view(Registration r) { Patient p=patients.findById(r.patientId).orElse(null); Doctor d=doctors.findById(r.doctorId).orElse(null); return new RegistrationView(r.id,r.visitNo,r.patientId,p==null?null:p.name,r.doctorId,d==null?null:d.realName,r.visitDate,r.registrationFee,r.status,r.registeredAt,r.consultationStartedAt,r.consultationEndedAt,r.cancelReason); }
    public record CreateRequest(@NotNull Long patientId,@NotNull Long doctorId,@NotNull LocalDate visitDate) {}
    public record CancelRequest(String reason) {}
    public record RegistrationView(Long id,String visitNo,Long patientId,String patientName,Long doctorId,String doctorName,LocalDate visitDate,java.math.BigDecimal registrationFee,RegistrationStatus status,LocalDateTime registeredAt,LocalDateTime consultationStartedAt,LocalDateTime consultationEndedAt,String cancelReason) {}
}
