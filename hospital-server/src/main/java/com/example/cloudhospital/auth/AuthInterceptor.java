package com.example.cloudhospital.auth;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component public class AuthInterceptor implements HandlerInterceptor {
 private final AuthService auth; public AuthInterceptor(AuthService auth){this.auth=auth;}
 @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler)throws Exception{
   if("OPTIONS".equals(req.getMethod())||req.getRequestURI().startsWith("/api/v1/auth/"))return true;
   String h=req.getHeader("Authorization");if(h==null||!h.startsWith("Bearer "))return deny(res,401,"请先登录");AuthService.Session s;try{s=auth.byToken(h.substring(7));}catch(Exception e){return deny(res,401,"登录已失效，请重新登录");}
   boolean patient=req.getRequestURI().startsWith("/api/v1/patient-portal/");if(patient&&!"PATIENT".equals(s.role()))return deny(res,403,"仅患者可访问此服务");if(!patient&&!"ADMIN".equals(s.role()))return deny(res,403,"仅院内管理员可访问此服务");req.setAttribute("session",s);return true;
 }
 private boolean deny(HttpServletResponse res,int status,String msg)throws Exception{res.setStatus(status);res.setCharacterEncoding("UTF-8");res.setContentType("application/json");res.getWriter().write("{\"code\":"+status+"00,\"message\":\""+msg+"\",\"data\":null}");return false;}
}
