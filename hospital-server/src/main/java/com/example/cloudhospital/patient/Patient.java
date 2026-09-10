package com.example.cloudhospital.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient {
  public Long id;
  public String patientNo;
  public String idCard;
  public String name;
  public String gender;
  public LocalDate birthday;
  public String phone;
  public String address;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
}
