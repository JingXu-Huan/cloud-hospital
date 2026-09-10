package com.example.cloudhospital.doctor;

import com.example.cloudhospital.common.ApiResponse;
import com.example.cloudhospital.common.BizException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {
  private final DoctorRepository doctors;
  private final com.example.cloudhospital.auth.AuthService auth;

  public DoctorController(
      DoctorRepository doctors, com.example.cloudhospital.auth.AuthService auth) {
    this.doctors = doctors;
    this.auth = auth;
  }

  @PostMapping
  @Transactional
  public ApiResponse<Doctor> create(@Valid @RequestBody CreateRequest req) {
    if (doctors.findByLoginName(req.loginName()).isPresent()
        || auth.usernameExists(req.loginName())) throw new BizException(40003, "登录名已存在");
    Doctor doctor = new Doctor();
    doctor.doctorNo =
        "D"
            + LocalDate.now().toString().replace("-", "")
            + String.format("%06d", new Random().nextInt(1_000_000));
    doctor.loginName = req.loginName();
    doctor.passwordHash = "{noop}123456";
    doctor.realName = req.realName();
    doctor.departmentName = req.departmentName();
    doctor.title = req.title();
    doctor.enabled = true;
    doctors.save(doctor);
    auth.provisionDoctorAccount(doctor.loginName);
    return ApiResponse.ok(doctor);
  }

  @GetMapping
  public ApiResponse<List<Doctor>> list(
      @RequestParam(required = false) String departmentName,
      @RequestParam(required = false) Boolean enabled) {
    if (departmentName != null && enabled != null)
      return ApiResponse.ok(
          doctors.findByDepartmentNameAndEnabledOrderByRealNameAsc(departmentName, enabled));
    if (departmentName != null)
      return ApiResponse.ok(doctors.findByDepartmentNameOrderByRealNameAsc(departmentName));
    return ApiResponse.ok(
        enabled == null
            ? doctors.findAll()
            : doctors.findByEnabledOrderByDepartmentNameAscRealNameAsc(enabled));
  }

  @GetMapping("/{id}")
  public ApiResponse<Doctor> detail(@PathVariable Long id) {
    return ApiResponse.ok(doctors.findById(id).orElseThrow(() -> new BizException(40002, "医生不存在")));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ApiResponse<Void> delete(@PathVariable Long id) {
    Doctor doctor =
        doctors.findById(id).orElseThrow(() -> new BizException(40002, "医生不存在"));
    if (doctors.existsRegistrationByDoctorId(id))
      throw new BizException(40004, "医生已有挂号记录，不能删除");
    auth.removeDoctorAccount(doctor.loginName);
    if (doctors.deleteById(id) != 1) throw new BizException(40002, "医生不存在");
    return ApiResponse.ok();
  }

  public record CreateRequest(
      @NotBlank String loginName,
      @NotBlank String realName,
      @NotBlank String departmentName,
      @NotBlank String title) {}
}
