package com.modernizedkitechensink.kitchensinkmodernized.audit;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AOP Aspect for automatic audit logging.
 *
 * Intercepts service methods and logs:
 * - Successful operations (@AfterReturning)
 * - Failed operations (@AfterThrowing)
 *
 * No changes needed to service code - AOP weaves in the logging.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

  private final AuditService auditService;

  // ============ POINTCUTS (WHERE to apply) ============

  /**
   * Matches MemberServiceImpl.register() method
   */
  @Pointcut("execution(* com.modernizedkitechensink.kitchensinkmodernized.service.MemberServiceImpl.register(..))")
  public void memberRegistration() {}

  /**
   * Matches MemberServiceImpl.update() method
   */
  @Pointcut("execution(* com.modernizedkitechensink.kitchensinkmodernized.service.MemberServiceImpl.update(..))")
  public void memberUpdate() {}

  /**
   * Matches MemberServiceImpl.delete() method
   */
  @Pointcut("execution(* com.modernizedkitechensink.kitchensinkmodernized.service.MemberServiceImpl.delete(..))")
  public void memberDelete() {}

  /**
   * Matches all read operations (findAll, findById)
   */
  @Pointcut("execution(* com.modernizedkitechensink.kitchensinkmodernized.service.MemberServiceImpl.find*(..))")
  public void memberRead() {}

  // ============ ADVICE (WHAT to do) ============

  /**
   * Log after successful member registration.
   *
   * @AfterReturning - runs only if method completes successfully
   * 'returning' captures the return value
   */
  @AfterReturning(pointcut = "memberRegistration()", returning = "result")
  public void logMemberCreated(JoinPoint joinPoint, Object result) {
    if (result instanceof Member member) {
      auditService.logSuccess("CREATE", "Member", member.getId(),
        Map.of(
          "name", member.getName(),
          "email", member.getEmail()
        ));
      log.debug("Audit: Member created - {}", member.getId());
    }
  }

  /**
   * Log after successful member update.
   */
  @AfterReturning(pointcut = "memberUpdate()", returning = "result")
  public void logMemberUpdated(JoinPoint joinPoint, Object result) {
    if (result instanceof Member member) {
      auditService.logSuccess("UPDATE", "Member", member.getId(),
        Map.of("name", member.getName()));
      log.debug("Audit: Member updated - {}", member.getId());
    }
  }

  /**
   * Log after successful member deletion.
   */
  @AfterReturning("memberDelete()")
  public void logMemberDeleted(JoinPoint joinPoint) {
    // Get the ID from method arguments
    Object[] args = joinPoint.getArgs();
    if (args.length > 0 && args[0] instanceof String memberId) {
      auditService.logSuccess("DELETE", "Member", memberId);
      log.debug("Audit: Member deleted - {}", memberId);
    }
  }

  /**
   * Log failed operations.
   *
   * @AfterThrowing - runs when method throws exception
   * 'throwing' captures the exception
   */
  @AfterThrowing(
    pointcut = "memberRegistration() || memberUpdate() || memberDelete()",
    throwing = "exception"
  )
  public void logOperationFailed(JoinPoint joinPoint, Exception exception) {
    String methodName = joinPoint.getSignature().getName();
    String action = mapMethodToAction(methodName);

    // Try to get entity ID from arguments
    String entityId = extractEntityId(joinPoint.getArgs());

    auditService.logFailure(action, "Member", entityId, exception.getMessage());
    log.debug("Audit: {} failed - {}", action, exception.getMessage());
  }

  // ============ HELPER METHODS ============

  private String mapMethodToAction(String methodName) {
    return switch (methodName) {
      case "register" -> "CREATE";
      case "update" -> "UPDATE";
      case "delete" -> "DELETE";
      default -> "UNKNOWN";
    };
  }

  private String extractEntityId(Object[] args) {
    for (Object arg : args) {
      if (arg instanceof String) {
        return (String) arg;
      }
      if (arg instanceof Member member) {
        return member.getId();
      }
    }
    return null;
  }
}