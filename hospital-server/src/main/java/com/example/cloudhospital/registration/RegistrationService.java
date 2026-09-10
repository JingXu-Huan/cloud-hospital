package com.example.cloudhospital.registration;

import com.example.cloudhospital.common.BizException;
import com.example.cloudhospital.doctor.Doctor;
import com.example.cloudhospital.doctor.DoctorRepository;
import com.example.cloudhospital.patient.PatientRepository;
import com.example.cloudhospital.prescription.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
public class RegistrationService {
  private final RegistrationRepository registrations;
  private final PatientRepository patients;
  private final DoctorRepository doctors;
  private final PrescriptionRepository prescriptions;

  public RegistrationService(
      RegistrationRepository registrations,
      PatientRepository patients,
      DoctorRepository doctors,
      PrescriptionRepository prescriptions) {
    this.registrations = registrations;
    this.patients = patients;
    this.doctors = doctors;
    this.prescriptions = prescriptions;
  }

  @Transactional
  public Registration create(Long patientId, Long doctorId, LocalDate date) {
    patients.findById(patientId).orElseThrow(() -> new BizException(40001, "患者不存在"));
    Doctor doctor = doctors.findById(doctorId).orElseThrow(() -> new BizException(40002, "医生不存在"));
    if (!Boolean.TRUE.equals(doctor.enabled)) throw new BizException(40002, "医生已停用");
    if (registrations.existsByPatientIdAndDoctorIdAndVisitDateAndStatusIn(
        patientId,
        doctorId,
        date,
        List.of(
            RegistrationStatus.WAITING,
            RegistrationStatus.IN_PROGRESS,
            RegistrationStatus.COMPLETED))) throw new BizException(41002, "同日同一医生已有未取消挂号");
    Registration r = new Registration();
    r.visitNo =
        "V"
            + LocalDate.now().toString().replace("-", "")
            + String.format("%06d", new Random().nextInt(1_000_000));
    r.patientId = patientId;
    r.doctorId = doctorId;
    r.visitDate = date;
    r.registrationFee = BigDecimal.valueOf(8);
    r.status = RegistrationStatus.WAITING;
    r.registeredAt = LocalDateTime.now();
    registrations.save(r);
    return r;
  }

  @Transactional
  public void start(Long id) {
    if (registrations.startIfWaiting(
            id, LocalDateTime.now(), RegistrationStatus.WAITING, RegistrationStatus.IN_PROGRESS)
        != 1) throw new BizException(41001, "仅待接诊挂号可开始接诊（可能已被其他请求处理）");
  }

  @Transactional
  public void startForDoctor(Long id, Long doctorId) {
    getForDoctor(id, doctorId);
    start(id);
  }

  @Transactional
  public void cancel(Long id, String reason) {
    if (registrations.cancelIfWaiting(
            id, reason, RegistrationStatus.WAITING, RegistrationStatus.CANCELLED)
        != 1) throw new BizException(41001, "仅待接诊挂号可取消");
  }

  @Transactional
  public void delete(Long id) {
    Registration registration = get(id);
    if (registration.status != RegistrationStatus.WAITING)
      throw new BizException(41001, "仅待接诊且尚未产生诊疗数据的挂号可删除");
    if (registrations.deleteIfWaiting(id, RegistrationStatus.WAITING) != 1)
      throw new BizException(41001, "挂号状态已变化，请刷新后重试");
  }

  @Transactional
  public void complete(Long id) {
    get(id);
    if (registrations.completeIfInProgress(
            id, LocalDateTime.now(), RegistrationStatus.IN_PROGRESS, RegistrationStatus.COMPLETED)
        != 1) throw new BizException(41001, "仅接诊中挂号可完成");
  }

  public Registration get(Long id) {
    return registrations.findById(id).orElseThrow(() -> new BizException(41001, "挂号不存在"));
  }

  public Registration getForDoctor(Long id, Long doctorId) {
    Registration registration = get(id);
    if (!Objects.equals(registration.doctorId, doctorId))
      throw new BizException(40301, "无权操作其他医生的挂号");
    return registration;
  }

  public List<Registration> queue(Long doctorId, LocalDate date, RegistrationStatus status) {
    return registrations.findByDoctorIdAndVisitDateAndStatusInOrderByRegisteredAtAsc(
        doctorId,
        date,
        status == null
            ? List.of(RegistrationStatus.WAITING, RegistrationStatus.IN_PROGRESS)
            : List.of(status));
  }
}
