package com.example.cloudhospital.medicalrecord;

import java.time.LocalDateTime;

public class MedicalRecord {
    public Long id;
    public Long registrationId;
    public Long patientId;
    public Long doctorId;
    public String chiefComplaint;
    public String presentIllness;
    public String pastHistory;
    public String allergyHistory;
    public String physicalExam;
    public String diagnosis;
    public String advice;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
