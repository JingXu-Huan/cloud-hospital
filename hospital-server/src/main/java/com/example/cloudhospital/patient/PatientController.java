package com.example.cloudhospital.patient;

import com.example.cloudhospital.common.ApiResponse;
import com.example.cloudhospital.common.BizException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
  private final PatientRepository patients;
  private final com.example.cloudhospital.auth.AuthService auth;

  public PatientController(
      PatientRepository patients, com.example.cloudhospital.auth.AuthService auth) {
    this.patients = patients;
    this.auth = auth;
  }

  @PostMapping
  public ApiResponse<Patient> create(@Valid @RequestBody CreateRequest req) {
    if (patients.findByIdCard(req.idCard()).isPresent()) throw new BizException(40002, "身份证号已建档");
    Patient p = new Patient();
    p.patientNo =
        "P"
            + LocalDate.now().toString().replace("-", "")
            + String.format("%06d", new Random().nextInt(1_000_000));
    p.idCard = req.idCard();
    p.name = req.name();
    p.gender = req.gender();
    p.birthday = req.birthday();
    p.phone = req.phone();
    p.address = req.address();
    patients.save(p);
    return ApiResponse.ok(p);
  }

  @GetMapping("/by-id-card")
  public ApiResponse<Patient> byIdCard(@RequestParam String idCard) {
    return ApiResponse.ok(findByCard(idCard));
  }

  @GetMapping("/{id}")
  public ApiResponse<Patient> detail(@PathVariable Long id) {
    return ApiResponse.ok(
        patients.findById(id).orElseThrow(() -> new BizException(40001, "患者不存在")));
  }

  @GetMapping
  public ApiResponse<List<Patient>> list(@RequestParam(defaultValue = "") String keyword) {
    return ApiResponse.ok(
        patients.findTop20ByNameContainingOrPatientNoContainingOrderByCreatedAtDesc(
            keyword, keyword));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    patients.findById(id).orElseThrow(() -> new BizException(40001, "患者不存在"));
    if (auth.patientAccountExists(id)) throw new BizException(40004, "患者已注册患者端账号，不能删除");
    if (patients.existsRegistrationByPatientId(id))
      throw new BizException(40004, "患者已有挂号记录，不能删除");
    if (patients.deleteById(id) != 1) throw new BizException(40001, "患者不存在");
    return ApiResponse.ok();
  }

  private Patient findByCard(String idCard) {
    return patients.findByIdCard(idCard).orElseThrow(() -> new BizException(40001, "患者不存在"));
  }

  public record CreateRequest(
      @NotBlank String idCard,
      @NotBlank String name,
      @NotBlank String gender,
      LocalDate birthday,
      String phone,
      String address) {}
}
