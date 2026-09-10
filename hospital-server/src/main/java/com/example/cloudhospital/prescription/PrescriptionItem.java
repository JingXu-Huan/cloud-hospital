package com.example.cloudhospital.prescription;

import java.math.BigDecimal;

public class PrescriptionItem {
  public Long id;
  public Long prescriptionId;
  public String drugCode;
  public String drugName;
  public String specification;
  public String unit;
  public BigDecimal unitPrice;
  public Integer quantity;
  public BigDecimal itemAmount;
  public String dosage;
  public String frequency;
  public String route;
}
