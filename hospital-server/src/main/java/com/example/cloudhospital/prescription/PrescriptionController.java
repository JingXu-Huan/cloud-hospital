package com.example.cloudhospital.prescription;

import com.example.cloudhospital.common.*;
import com.example.cloudhospital.doctor.*;
import com.example.cloudhospital.patient.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController @RequestMapping("/api/v1")
public class PrescriptionController {
    private final PrescriptionService service; private final PatientRepository patients; private final DoctorRepository doctors;
    public PrescriptionController(PrescriptionService service, PatientRepository patients, DoctorRepository doctors) { this.service=service; this.patients=patients; this.doctors=doctors; }
    @PostMapping("/registrations/{registrationId}/prescriptions") public ApiResponse<PrescriptionView> create(@PathVariable Long registrationId,@Valid @RequestBody CreateRequest req) { return ApiResponse.ok(view(service.create(registrationId,req))); }
    @GetMapping("/prescriptions") public ApiResponse<List<PrescriptionView>> list(@RequestParam(required=false) PrescriptionStatus status) { return ApiResponse.ok(service.list(status).stream().map(this::view).toList()); }
    @GetMapping("/prescriptions/{id}") public ApiResponse<PrescriptionDetail> get(@PathVariable Long id) { Prescription p=service.get(id); return ApiResponse.ok(new PrescriptionDetail(view(p),service.items(id))); }
    @PostMapping("/prescriptions/{id}/pay") public ApiResponse<Void> pay(@PathVariable Long id,@Valid @RequestBody PayRequest req) { service.pay(id,req.paymentMethod()); return ApiResponse.ok(); }
    @PostMapping("/prescriptions/{id}/dispense") public ApiResponse<Void> dispense(@PathVariable Long id) { service.dispense(id); return ApiResponse.ok(); }
    @PostMapping("/prescriptions/{id}/cancel") public ApiResponse<Void> cancel(@PathVariable Long id) { service.cancel(id); return ApiResponse.ok(); }
    private PrescriptionView view(Prescription p) { Patient patient=patients.findById(p.patientId).orElse(null); Doctor doctor=doctors.findById(p.doctorId).orElse(null); return new PrescriptionView(p.id,p.prescriptionNo,p.registrationId,p.patientId,patient==null?null:patient.name,p.doctorId,doctor==null?null:doctor.realName,p.status,p.totalAmount,p.paymentMethod,p.prescribedAt,p.paidAt,p.dispensedAt,p.remark); }
    public record CreateRequest(@Valid @NotEmpty List<ItemRequest> items,String remark) {}
    public record ItemRequest(String drugCode,@NotBlank String drugName,String specification,@NotBlank String unit,@NotNull @DecimalMin("0.00") BigDecimal unitPrice,@NotNull @Min(1) Integer quantity,String dosage,String frequency,String route) {}
    public record PayRequest(@NotBlank String paymentMethod) {}
    public record PrescriptionView(Long id,String prescriptionNo,Long registrationId,Long patientId,String patientName,Long doctorId,String doctorName,PrescriptionStatus status,BigDecimal totalAmount,String paymentMethod,LocalDateTime prescribedAt,LocalDateTime paidAt,LocalDateTime dispensedAt,String remark) {}
    public record PrescriptionDetail(PrescriptionView prescription,List<PrescriptionItem> items) {}
}
