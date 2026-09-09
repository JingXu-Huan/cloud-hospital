package com.example.cloudhospital.auth;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component public class AuthInterceptor implements HandlerInterceptor {
 private final AuthService auth; public AuthInterceptor(AuthService auth){this.auth=auth;}
 @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler)throws Exception{
   if("OPTIONS".equals(req.getMethod())||req.getRequestURI().startsWith("/api/v1/auth/"))return true;
   String token=token(req);if(token==null)return deny(res,401,"请先登录");AuthService.Session s;try{s=auth.byToken(token);}catch(Exception e){return deny(res,401,"登录已失效，请重新登录");}
   String uri=req.getRequestURI();boolean patient=uri.startsWith("/api/v1/patient-portal/");boolean doctor=uri.startsWith("/api/v1/doctor-workstation/");
   if(patient&&!"PATIENT".equals(s.role()))return deny(res,403,"仅患者可访问此服务");
   if(doctor&&!"DOCTOR".equals(s.role()))return deny(res,403,"仅医生可访问医生工作站");
   if(!patient&&!doctor&&!"ADMIN".equals(s.role()))return deny(res,403,"仅院内管理员可访问此服务");req.setAttribute("session",s);return true;
 }
 private String token(HttpServletRequest req){String h=req.getHeader("Authorization");if(h!=null&&h.startsWith("Bearer "))return h.substring(7);if(req.getCookies()!=null)for(Cookie c:req.getCookies())if(TokenCookie.NAME.equals(c.getName()))return c.getValue();return null;}
 private boolean deny(HttpServletResponse res,int status,String msg)throws Exception{res.setStatus(status);res.setCharacterEncoding("UTF-8");res.setContentType("application/json");res.getWriter().write("{\"code\":"+status+"00,\"message\":\""+msg+"\",\"data\":null}");return false;}
}
