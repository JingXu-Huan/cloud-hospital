package com.example.cloudhospital.auth;

import com.example.cloudhospital.common.BizException;
import com.example.cloudhospital.patient.Patient;
import com.example.cloudhospital.patient.PatientRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final JdbcTemplate jdbc; private final PatientRepository patients;
  public AuthService(JdbcTemplate jdbc, PatientRepository patients){this.jdbc=jdbc;this.patients=patients;}
  @Transactional public Session login(String username,String password){
    List<Session> rows=jdbc.query("SELECT id,username,role,patient_id FROM user_account WHERE username=? AND password_hash=SHA2(?,256)",(rs,n)->new Session(rs.getLong("id"),rs.getString("username"),rs.getString("role"),rs.getObject("patient_id",Long.class),null),username,password);
    if(rows.isEmpty())throw new BizException(40100,"账号或密码不正确"); Session s=rows.get(0);String token=UUID.randomUUID().toString();jdbc.update("UPDATE user_account SET auth_token=? WHERE id=?",token,s.id());return new Session(s.id(),s.username(),s.role(),s.patientId(),token);
  }
  @Transactional public Session register(RegisterRequest r){
    if(!jdbc.queryForList("SELECT id FROM user_account WHERE username=?",r.username()).isEmpty())throw new BizException(40003,"用户名已存在");
    Patient p=patients.findByIdCard(r.idCard()).orElse(null);
    if(p==null){p=new Patient();p.patientNo="P"+LocalDate.now().toString().replace("-","")+String.format("%06d",new Random().nextInt(1_000_000));p.idCard=r.idCard();p.name=r.name();p.gender=r.gender();p.birthday=r.birthday();p.phone=r.phone();p.address=r.address();patients.save(p);}
    else if(!p.name.equals(r.name()) || !p.phone.equals(r.phone())) throw new BizException(40002,"身份信息与已有档案不一致");
    if(!jdbc.queryForList("SELECT id FROM user_account WHERE patient_id=?",p.id).isEmpty())throw new BizException(40003,"该患者已注册，请直接登录");
    String token=UUID.randomUUID().toString();jdbc.update("INSERT INTO user_account(username,password_hash,role,patient_id,auth_token) VALUES (?,SHA2(?,256),'PATIENT',?,?)",r.username(),r.password(),p.id,token);Long id=jdbc.queryForObject("SELECT id FROM user_account WHERE username=?",Long.class,r.username());return new Session(id,r.username(),"PATIENT",p.id,token);
  }
  public Session byToken(String token){List<Session>x=jdbc.query("SELECT id,username,role,patient_id,auth_token FROM user_account WHERE auth_token=?",(rs,n)->new Session(rs.getLong("id"),rs.getString("username"),rs.getString("role"),rs.getObject("patient_id",Long.class),rs.getString("auth_token")),token);if(x.isEmpty())throw new BizException(40101,"登录已失效，请重新登录");return x.get(0);}
  public record Session(Long id,String username,String role,Long patientId,String token){}
  public record RegisterRequest(String username,String password,String idCard,String name,String gender,LocalDate birthday,String phone,String address){}
}
