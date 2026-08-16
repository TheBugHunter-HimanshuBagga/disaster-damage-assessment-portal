package com.himanshuProjects.disaster_damage_assessment_portal.aspect;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.AuditLog;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.AuditLogRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogAspect(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Around("@annotation(com.himanshuProjects.disaster_damage_assessment_portal.aspect.Auditable)")
    @Transactional
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Auditable auditable = method.getAnnotation(Auditable.class);

            String email = getCurrentUserEmail();
            if (email == null) return result;

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return result;

            String ipAddress = getClientIp();

            Long entityId = extractEntityId(result, joinPoint.getArgs());

            String description = buildDescription(auditable, joinPoint.getArgs(), result);

            AuditLog auditLog = new AuditLog();
            auditLog.setAction(auditable.action());
            auditLog.setEntityName(auditable.entityName());
            auditLog.setEntityId(entityId != null ? entityId : 0L);
            auditLog.setDescription(description);
            auditLog.setIpAddress(ipAddress);
            auditLog.setPerformedBy(user);
            auditLog.setPerformedAt(LocalDateTime.now());

            auditLogRepository.save(auditLog);
            log.info("Audit log saved: {} {} by {}", auditable.action(), auditable.entityName(), email);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }

        return result;
    }

    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Could not extract current user from security context");
        }
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not extract client IP");
        }
        return "UNKNOWN";
    }

    private Long extractEntityId(Object result, Object[] args) {
        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof Long longId) {
                    return longId;
                }
            } catch (Exception ignored) {
            }
        }

        for (Object arg : args) {
            if (arg instanceof Long longArg) {
                return longArg;
            }
            try {
                Method getIdMethod = arg.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(arg);
                if (id instanceof Long longId) {
                    return longId;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String buildDescription(Auditable auditable, Object[] args, Object result) {
        String desc = auditable.description();
        if (desc != null && !desc.isEmpty()) {
            return desc;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(auditable.action()).append(" performed on ").append(auditable.entityName());

        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                sb.append(" (ID: ").append(id).append(")");
            } catch (Exception ignored) {
            }
        }

        return sb.toString();
    }
}
