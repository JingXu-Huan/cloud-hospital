package com.example.cloudhospital.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthMapper {
  List<AuthService.Session> findSessionsByUsernameAndPassword(
      @Param("username") String username, @Param("password") String password);

  int updateToken(@Param("id") Long id, @Param("token") String token);

  boolean existsByUsername(@Param("username") String username);

  boolean existsByPatientId(@Param("patientId") Long patientId);

  int insertPatientAccount(
      @Param("username") String username,
      @Param("password") String password,
      @Param("patientId") Long patientId,
      @Param("token") String token);

  int insertDoctorAccount(
      @Param("username") String username,
      @Param("password") String password,
      @Param("token") String token);

  int deleteDoctorAccount(@Param("username") String username);

  Long findIdByUsername(@Param("username") String username);

  List<AuthService.Session> findSessionsByToken(@Param("token") String token);
}
