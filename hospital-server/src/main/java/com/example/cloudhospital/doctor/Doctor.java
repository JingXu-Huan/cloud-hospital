package com.example.cloudhospital.doctor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

public class Doctor {
    public Long id;
    public String doctorNo;
    public String loginName;
    @JsonIgnore public String passwordHash;
    public String realName;
    public String departmentName;
    public String title; public Boolean enabled;
    public LocalDateTime createdAt;
}
