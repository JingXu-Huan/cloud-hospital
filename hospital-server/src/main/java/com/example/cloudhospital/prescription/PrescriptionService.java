package com.example.cloudhospital.prescription;

import com.example.cloudhospital.common.BizException;
import com.example.cloudhospital.registration.Registration;
import com.example.cloudhospital.registration.RegistrationService;
import com.example.cloudhospital.registration.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class PrescriptionService {
  private final PrescriptionRepository prescriptions;
  private final PrescriptionItemRepository items;
  private final RegistrationService registrations;
  private final PrescriptionNotificationMapper notificationMapper;

  public PrescriptionService(
      PrescriptionRepository prescriptions,
      PrescriptionItemRepository items,
      RegistrationService registrations,
      PrescriptionNotificationMapper notificationMapper) {
    this.prescriptions = prescriptions;
    this.items = items;
    this.registrations = registrations;
    this.notificationMapper = notificationMapper;
  }

  @Transactional
  public Prescription create(Long registrationId, PrescriptionController.CreateRequest req) {
    Registration r = registrations.get(registrationId);
    if (r.status != RegistrationStatus.IN_PROGRESS) throw new BizException(43002, "仅接诊中挂号可以开具处方");
    if (req.items() == null || req.items().isEmpty()) throw new BizException(43001, "处方至少包含一个药品");
    List<PrescriptionItem> entities = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    for (PrescriptionController.ItemRequest it : req.items()) {
      if (it.quantity() == null
          || it.quantity() <= 0
          || it.unitPrice() == null
          || it.unitPrice().compareTo(BigDecimal.ZERO) < 0)
        throw new BizException(43001, "药品数量或单价不合法");
      BigDecimal amount =
          it.unitPrice()
              .multiply(BigDecimal.valueOf(it.quantity()))
              .setScale(2, RoundingMode.HALF_UP);
      total = total.add(amount);
      PrescriptionItem x = new PrescriptionItem();
      x.drugCode = it.drugCode();
      x.drugName = it.drugName();
      x.specification = it.specification();
      x.unit = it.unit();
      x.unitPrice = it.unitPrice().setScale(2, RoundingMode.HALF_UP);
      x.quantity = it.quantity();
      x.itemAmount = amount;
      x.dosage = it.dosage();
      x.frequency = it.frequency();
      x.route = it.route();
      entities.add(x);
    }
    Prescription p = new Prescription();
    p.prescriptionNo =
        "RX"
            + LocalDate.now().toString().replace("-", "")
            + String.format("%06d", new Random().nextInt(1_000_000));
    p.registrationId = registrationId;
    p.patientId = r.patientId;
    p.doctorId = r.doctorId;
    p.status = PrescriptionStatus.UNPAID;
    p.totalAmount = total;
    p.prescribedAt = LocalDateTime.now();
    p.remark = req.remark();
    prescriptions.save(p);
    for (PrescriptionItem x : entities) x.prescriptionId = p.id;
    items.saveAll(entities);
    registrations.complete(registrationId);
    return p;
  }

  @Transactional
  public Prescription createForDoctor(
      Long registrationId, Long doctorId, PrescriptionController.CreateRequest req) {
    registrations.getForDoctor(registrationId, doctorId);
    return create(registrationId, req);
  }

  public Prescription get(Long id) {
    return prescriptions.findById(id).orElseThrow(() -> new BizException(43002, "处方不存在"));
  }

  public boolean hasPatientAccount(Long patientId) {
    return notificationMapper.countPatientAccounts(patientId) > 0;
  }

  public List<Prescription> list(PrescriptionStatus status) {
    return status == null
        ? prescriptions.findAll()
        : prescriptions.findByStatusOrderByPrescribedAtAsc(status);
  }

  public List<PrescriptionItem> items(Long id) {
    get(id);
    return items.findByPrescriptionId(id);
  }

  @Transactional
  public void pay(Long id, String method) {
    if (method == null || method.isBlank()) throw new BizException(40000, "请选择支付方式");
    if (prescriptions.payIfUnpaid(
            id, method, LocalDateTime.now(), PrescriptionStatus.UNPAID, PrescriptionStatus.PAID)
        != 1) throw new BizException(43003, "处方非待缴费状态，不能重复缴费");
  }

  @Transactional
  public void manuallyPayForUnregisteredPatient(Long id) {
    Prescription prescription = get(id);
    if (hasPatientAccount(prescription.patientId))
      throw new BizException(43005, "该患者已注册患者端，请由患者本人完成支付");
    pay(id, "CASH");
  }

  @Transactional
  public void dispense(Long id) {
    if (prescriptions.dispenseIfPaid(
            id, LocalDateTime.now(), PrescriptionStatus.PAID, PrescriptionStatus.DISPENSED)
        != 1) throw new BizException(43004, "仅已缴费处方可以发药");
    Prescription p = get(id);
    notificationMapper.insertPickupReadyNotification(p.patientId, id, p.prescriptionNo);
  }

  @Transactional
  public void pickUp(Long id) {
    if (prescriptions.pickUpIfDispensed(
            id, LocalDateTime.now(), PrescriptionStatus.DISPENSED, PrescriptionStatus.PICKED_UP)
        != 1) throw new BizException(43006, "仅已发药处方可以确认取药");
    notificationMapper.markNotificationsReadByPrescriptionId(id);
  }

  @Transactional
  public void cancel(Long id) {
    if (prescriptions.cancelIfUnpaid(id, PrescriptionStatus.UNPAID, PrescriptionStatus.CANCELLED)
        != 1) throw new BizException(43002, "仅未支付处方可以作废");
  }
}
