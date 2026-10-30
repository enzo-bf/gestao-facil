package com.gestaofacil.security;

import com.gestaofacil.dto.BudgetResponseDTO;
import com.gestaofacil.dto.ClientResponseDTO;
import com.gestaofacil.dto.ServiceOrderResponseDTO;
import com.gestaofacil.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class BusinessAuditAspect {

    private final AuditService auditService;

    @Around("""
            execution(* com.gestaofacil.service.ClientService.create(..)) ||
            execution(* com.gestaofacil.service.ClientService.update(..)) ||
            execution(* com.gestaofacil.service.ClientService.delete(..)) ||
            execution(* com.gestaofacil.service.ServiceOrderService.create(..)) ||
            execution(* com.gestaofacil.service.ServiceOrderService.update(..)) ||
            execution(* com.gestaofacil.service.ServiceOrderService.updateStatus(..)) ||
            execution(* com.gestaofacil.service.ServiceOrderService.delete(..)) ||
            execution(* com.gestaofacil.service.BudgetService.create(..)) ||
            execution(* com.gestaofacil.service.BudgetService.update(..)) ||
            execution(* com.gestaofacil.service.BudgetService.updateStatus(..)) ||
            execution(* com.gestaofacil.service.BudgetService.delete(..))
            """)
    public Object recordBusinessMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        Object[] args = joinPoint.getArgs();
        String actorEmail = (String) args[0];
        String method = joinPoint.getSignature().getName();
        String resource = switch (joinPoint.getSignature().getDeclaringType().getSimpleName()) {
            case "ClientService" -> "CLIENT";
            case "ServiceOrderService" -> "SERVICE_ORDER";
            case "BudgetService" -> "BUDGET";
            default -> throw new IllegalStateException("Mutation audit resource is not mapped: "
                    + joinPoint.getSignature().getDeclaringType().getSimpleName());
        };
        String action = switch (method) {
            case "create" -> "CREATE";
            case "update" -> "UPDATE";
            case "updateStatus" -> "UPDATE_STATUS";
            case "delete" -> "DELETE";
            default -> throw new IllegalStateException("Mutation audit action is not mapped: " + method);
        };
        String resourceId = result instanceof ClientResponseDTO client ? client.id().toString()
                : result instanceof ServiceOrderResponseDTO order ? order.id().toString()
                : result instanceof BudgetResponseDTO budget ? budget.id().toString()
                : args.length > 1 && args[1] instanceof UUID id ? id.toString() : null;
        auditService.record(actorEmail, action, resource, resourceId);
        return result;
    }
}
