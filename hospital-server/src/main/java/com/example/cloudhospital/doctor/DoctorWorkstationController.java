package com.example.cloudhospital.doctor;

import com.example.cloudhospital.auth.AuthService;
import com.example.cloudhospital.common.ApiResponse;
import com.example.cloudhospital.common.BizException;
import com.example.cloudhospital.medicalrecord.MedicalRecord;
import com.example.cloudhospital.medicalrecord.MedicalRecordController;
import com.example.cloudhospital.medicalrecord.MedicalRecordService;
import com.example.cloudhospital.patient.Patient;
import com.example.cloudhospital.patient.PatientRepository;
import com.example.cloudhospital.prescription.Prescription;
import com.example.cloudhospital.prescription.PrescriptionController;
import com.example.cloudhospital.prescription.PrescriptionService;
import com.example.cloudhospital.registration.Registration;
import com.example.cloudhospital.registration.RegistrationService;
import com.example.cloudhospital.registration.RegistrationStatus;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctor-workstation")
public class DoctorWorkstationController {
    private final RegistrationService registrations;
    private final MedicalRecordService records;
    private final PrescriptionService prescriptions;
    private final PatientRepository patients;

    public DoctorWorkstationController(RegistrationService registrations, MedicalRecordService records, PrescriptionService prescriptions, PatientRepository patients) {
        this.registrations = registrations;
        this.records = records;
        this.prescriptions = prescriptions;
        this.patients = patients;
    }

    @GetMapping("/queue")
    public ApiResponse<List<QueueItem>> queue(@RequestAttribute("session") AuthService.Session session, @RequestParam(required = false) LocalDate visitDate) {
        Long doctorId = doctorId(session);
        return ApiResponse.ok(registrations.queue(doctorId, visitDate == null ? LocalDate.now() : visitDate, null).stream().map(this::queueItem).toList());
    }

    @PostMapping("/registrations/{registrationId}/start")
    public ApiResponse<Void> start(@RequestAttribute("session") AuthService.Session session, @PathVariable Long registrationId) {
        registrations.startForDoctor(registrationId, doctorId(session));
        return ApiResponse.ok();
    }

    @GetMapping("/registrations/{registrationId}/medical-record")
    public ApiResponse<MedicalRecord> getRecord(@RequestAttribute("session") AuthService.Session session, @PathVariable Long registrationId) {
        return ApiResponse.ok(records.getForDoctor(registrationId, doctorId(session)));
    }

    @PutMapping("/registrations/{registrationId}/medical-record")
    public ApiResponse<MedicalRecord> saveRecord(@RequestAttribute("session") AuthService.Session session, @PathVariable Long registrationId, @Valid @RequestBody MedicalRecordController.SaveRequest request) {
        return ApiResponse.ok(records.saveForDoctor(registrationId, doctorId(session), request));
    }

    @PostMapping("/registrations/{registrationId}/prescriptions")
    public ApiResponse<PrescriptionView> createPrescription(@RequestAttribute("session") AuthService.Session session, @PathVariable Long registrationId, @Valid @RequestBody PrescriptionController.CreateRequest request) {
        Prescription prescription = prescriptions.createForDoctor(registrationId, doctorId(session), request);
        return ApiResponse.ok(new PrescriptionView(prescription.id, prescription.prescriptionNo, prescription.status, prescription.totalAmount, prescription.prescribedAt));
    }

    private Long doctorId(AuthService.Session session) {
        if (session.doctorId() == null) throw new BizException(40301, "当前医生账户未关联有效医生档案");
        return session.doctorId();
    }

    private QueueItem queueItem(Registration registration) {
        Patient patient = patients.findById(registration.patientId).orElse(null);
        return new QueueItem(registration.id, registration.visitNo, patient == null ? null : patient.name, registration.status, registration.registeredAt);
    }

    public record QueueItem(Long id, String visitNo, String patientName, RegistrationStatus status, LocalDateTime registeredAt) {}
    public record PrescriptionView(Long id, String prescriptionNo, com.example.cloudhospital.prescription.PrescriptionStatus status, BigDecimal totalAmount, LocalDateTime prescribedAt) {}
}
