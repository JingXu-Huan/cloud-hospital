package com.example.cloudhospital.auth;
import com.example.cloudhospital.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth") public class AuthController {
 private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
 @PostMapping("/login") public ApiResponse<AuthService.Session> login(@Valid @RequestBody Login r){return ApiResponse.ok(auth.login(r.username(),r.password()));}
 @PostMapping("/register") public ApiResponse<AuthService.Session> register(@Valid @RequestBody Register r){return ApiResponse.ok(auth.register(new AuthService.RegisterRequest(r.username(),r.password(),r.idCard(),r.name(),r.gender(),r.birthday(),r.phone(),r.address())));}
 public record Login(@NotBlank String username,@NotBlank String password){} public record Register(@NotBlank String username,@Size(min=6) String password,@NotBlank String idCard,@NotBlank String name,@Pattern(regexp="MALE|FEMALE") String gender,LocalDate birthday,@NotBlank String phone,String address){}
}
