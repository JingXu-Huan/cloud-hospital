package com.example.cloudhospital.auth;

import com.example.cloudhospital.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/login")
  public ApiResponse<AuthService.Session> login(
      @Valid @RequestBody Login r, HttpServletResponse response) {
    AuthService.Session session = auth.login(r.username(), r.password());
    TokenCookie.write(response, session.token());
    return ApiResponse.ok(session);
  }

  @PostMapping("/register")
  public ApiResponse<AuthService.Session> register(
      @Valid @RequestBody Register r, HttpServletResponse response) {
    AuthService.Session session =
        auth.register(
            new AuthService.RegisterRequest(
                r.username(),
                r.password(),
                r.idCard(),
                r.name(),
                r.gender(),
                r.birthday(),
                r.phone(),
                r.address()));
    TokenCookie.write(response, session.token());
    return ApiResponse.ok(session);
  }

  @PostMapping("/doctor-register")
  public ApiResponse<AuthService.Session> doctorRegister(
      @Valid @RequestBody DoctorRegister r, HttpServletResponse response) {
    AuthService.Session session =
        auth.registerDoctor(
            new AuthService.DoctorRegisterRequest(
                r.username(), r.password(), r.realName(), r.departmentName(), r.title()));
    TokenCookie.write(response, session.token());
    return ApiResponse.ok(session);
  }

  public record Login(@NotBlank String username, @NotBlank String password) {}

  public record Register(
      @NotBlank String username,
      @Size(min = 6) String password,
      @NotBlank String idCard,
      @NotBlank String name,
      @Pattern(regexp = "MALE|FEMALE") String gender,
      LocalDate birthday,
      @NotBlank String phone,
      String address) {}

  public record DoctorRegister(
      @NotBlank String username,
      @Size(min = 6) String password,
      @NotBlank String realName,
      @NotBlank String departmentName,
      @NotBlank String title) {}
}
