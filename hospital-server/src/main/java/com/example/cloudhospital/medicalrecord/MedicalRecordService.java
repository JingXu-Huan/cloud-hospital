package com.example.cloudhospital.medicalrecord;

import com.example.cloudhospital.common.BizException;
import com.example.cloudhospital.registration.Registration;
import com.example.cloudhospital.registration.RegistrationService;
import com.example.cloudhospital.registration.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MedicalRecordService {
  private final MedicalRecordRepository records;
  private final RegistrationService registrations;

  public MedicalRecordService(MedicalRecordRepository records, RegistrationService registrations) {
    this.records = records;
    this.registrations = registrations;
  }

  @Transactional
  public MedicalRecord save(Long registrationId, MedicalRecordController.SaveRequest req) {
    Registration r = registrations.get(registrationId);
    if (r.status != RegistrationStatus.IN_PROGRESS) throw new BizException(42001, "未开始接诊，不能保存病历");
    MedicalRecord m =
        records
            .findByRegistrationId(registrationId)
            .orElseGet(
                () -> {
                  MedicalRecord n = new MedicalRecord();
                  n.registrationId = registrationId;
                  n.patientId = r.patientId;
                  n.doctorId = r.doctorId;
                  n.createdAt = LocalDateTime.now();
                  return n;
                });
    m.chiefComplaint = req.chiefComplaint();
    m.presentIllness = req.presentIllness();
    m.pastHistory = req.pastHistory();
    m.allergyHistory = req.allergyHistory();
    m.physicalExam = req.physicalExam();
    m.diagnosis = req.diagnosis();
    m.advice = req.advice();
    m.updatedAt = LocalDateTime.now();
    records.save(m);
    return m;
  }

  public MedicalRecord get(Long registrationId) {
    return records.findByRegistrationId(registrationId).orElse(null);
  }

  @Transactional
  public void delete(Long registrationId) {
    Registration registration = registrations.get(registrationId);
    if (registration.status != RegistrationStatus.IN_PROGRESS)
      throw new BizException(42001, "仅接诊中的病历可删除");
    if (records.deleteByRegistrationId(registrationId) != 1)
      throw new BizException(42001, "病历不存在或已被删除");
  }

  @Transactional
  public MedicalRecord saveForDoctor(
      Long registrationId, Long doctorId, MedicalRecordController.SaveRequest req) {
    registrations.getForDoctor(registrationId, doctorId);
    return save(registrationId, req);
  }

  public MedicalRecord getForDoctor(Long registrationId, Long doctorId) {
    registrations.getForDoctor(registrationId, doctorId);
    return get(registrationId);
  }

  @Transactional
  public void deleteForDoctor(Long registrationId, Long doctorId) {
    registrations.getForDoctor(registrationId, doctorId);
    delete(registrationId);
  }
}
