package com.example.cloudhospital.registration;

import java.math.BigDecimal;
import java.time.*;

public class Registration {
    public Long id;
    public String visitNo;
    public Long patientId;
    public Long doctorId;
    public LocalDate visitDate;
    public BigDecimal registrationFee;
    public RegistrationStatus status;
    public LocalDateTime registeredAt;
    public LocalDateTime consultationStartedAt;
    public LocalDateTime consultationEndedAt;
    public String cancelReason;
}
