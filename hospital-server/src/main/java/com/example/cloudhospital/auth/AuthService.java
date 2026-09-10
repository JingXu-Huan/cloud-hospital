package com.example.cloudhospital.auth;

import com.example.cloudhospital.common.BizException;
import com.example.cloudhospital.doctor.Doctor;
import com.example.cloudhospital.doctor.DoctorRepository;
import com.example.cloudhospital.patient.Patient;
import com.example.cloudhospital.patient.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {
  private final AuthMapper authMapper;
  private final PatientRepository patients;
  private final DoctorRepository doctors;

  public AuthService(AuthMapper authMapper, PatientRepository patients, DoctorRepository doctors) {
    this.authMapper = authMapper;
    this.patients = patients;
    this.doctors = doctors;
  }

  @Transactional
  public Session login(String username, String password) {
    List<Session> rows = authMapper.findSessionsByUsernameAndPassword(username, password);
    if (rows.isEmpty()) throw new BizException(40100, "账号或密码不正确");
    Session s = rows.get(0);
    String token = UUID.randomUUID().toString();
    authMapper.updateToken(s.id(), token);
    return new Session(
        s.id(),
        s.username(),
        s.role(),
        s.patientId(),
        s.doctorId(),
        s.doctorName(),
        s.departmentName(),
        s.doctorTitle(),
        token);
  }

  @Transactional
  public Session register(RegisterRequest r) {
    if (authMapper.existsByUsername(r.username())) throw new BizException(40003, "用户名已存在");
    Patient p = patients.findByIdCard(r.idCard()).orElse(null);
    if (p == null) {
      p = new Patient();
      p.patientNo =
          "P"
              + LocalDate.now().toString().replace("-", "")
              + String.format("%06d", new Random().nextInt(1_000_000));
      p.idCard = r.idCard();
      p.name = r.name();
      p.gender = r.gender();
      p.birthday = r.birthday();
      p.phone = r.phone();
      p.address = r.address();
      patients.save(p);
    } else if (!p.name.equals(r.name()) || !p.phone.equals(r.phone()))
      throw new BizException(40002, "身份信息与已有档案不一致");
    if (authMapper.existsByPatientId(p.id)) throw new BizException(40003, "该患者已注册，请直接登录");
    String token = UUID.randomUUID().toString();
    authMapper.insertPatientAccount(r.username(), r.password(), p.id, token);
    Long id = authMapper.findIdByUsername(r.username());
    return new Session(id, r.username(), "PATIENT", p.id, null, null, null, null, token);
  }

  @Transactional
  public Session registerDoctor(DoctorRegisterRequest r) {
    if (authMapper.existsByUsername(r.username())
        || doctors.findByLoginName(r.username()).isPresent())
      throw new BizException(40003, "用户名已存在");
    Doctor doctor = new Doctor();
    doctor.doctorNo =
        "D"
            + LocalDate.now().toString().replace("-", "")
            + String.format("%06d", new Random().nextInt(1_000_000));
    doctor.loginName = r.username();
    doctor.passwordHash = "{managed-by-user-account}";
    doctor.realName = r.realName();
    doctor.departmentName = r.departmentName();
    doctor.title = r.title();
    doctor.enabled = true;
    doctors.save(doctor);
    String token = UUID.randomUUID().toString();
    authMapper.insertDoctorAccount(r.username(), r.password(), token);
    Long id = authMapper.findIdByUsername(r.username());
    return new Session(
        id,
        r.username(),
        "DOCTOR",
        null,
        doctor.id,
        doctor.realName,
        doctor.departmentName,
        doctor.title,
        token);
  }

  public boolean usernameExists(String username) {
    return authMapper.existsByUsername(username);
  }

  public boolean patientAccountExists(Long patientId) {
    return authMapper.existsByPatientId(patientId);
  }

  public void provisionDoctorAccount(String username) {
    authMapper.insertDoctorAccount(username, "123456", null);
  }

  public void removeDoctorAccount(String username) {
    authMapper.deleteDoctorAccount(username);
  }

  public Session byToken(String token) {
    List<Session> x = authMapper.findSessionsByToken(token);
    if (x.isEmpty()) throw new BizException(40101, "登录已失效，请重新登录");
    return x.get(0);
  }

  @Transactional
  public String renew(Session session) {
    String token = UUID.randomUUID().toString();
    authMapper.updateToken(session.id(), token);
    return token;
  }

  public record Session(
      Long id,
      String username,
      String role,
      Long patientId,
      Long doctorId,
      String doctorName,
      String departmentName,
      String doctorTitle,
      String token) {}

  public record RegisterRequest(
      String username,
      String password,
      String idCard,
      String name,
      String gender,
      LocalDate birthday,
      String phone,
      String address) {}

  public record DoctorRegisterRequest(
      String username, String password, String realName, String departmentName, String title) {}
}
