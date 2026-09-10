package com.example.cloudhospital.medicalrecord;

import com.example.cloudhospital.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/registrations/{registrationId}/medical-record")
public class MedicalRecordController {
  private final MedicalRecordService service;

  public MedicalRecordController(MedicalRecordService service) {
    this.service = service;
  }

  @PutMapping
  public ApiResponse<MedicalRecord> save(
      @PathVariable Long registrationId, @Valid @RequestBody SaveRequest req) {
    return ApiResponse.ok(service.save(registrationId, req));
  }

  @GetMapping
  public ApiResponse<MedicalRecord> get(@PathVariable Long registrationId) {
    return ApiResponse.ok(service.get(registrationId));
  }

  @DeleteMapping
  public ApiResponse<Void> delete(@PathVariable Long registrationId) {
    service.delete(registrationId);
    return ApiResponse.ok();
  }

  public record SaveRequest(
      String chiefComplaint,
      String presentIllness,
      String pastHistory,
      String allergyHistory,
      String physicalExam,
      @NotBlank String diagnosis,
      String advice) {}
}
