package com.example.cloudhospital.prescription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Prescription {
  public Long id;
  public String prescriptionNo;
  public Long registrationId;
  public Long patientId;
  public Long doctorId;
  public PrescriptionStatus status;
  public BigDecimal totalAmount;
  public String paymentMethod;
  public LocalDateTime prescribedAt;
  public LocalDateTime paidAt;
  public LocalDateTime dispensedAt;
  public LocalDateTime pickedUpAt;
  public String remark;
}
