package com.company.resourceallocation.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.company.resourceallocation.core..*Service.create*(..)) || " +
            "execution(* com.company.resourceallocation.core..*Service.update*(..)) || " +
            "execution(* com.company.resourceallocation.core..*Service.delete*(..))")
    public Object logAction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        
        // Derive Entity Name (e.g., EmployeeServiceImpl -> Employee, EmployeeService -> Employee)
        String entityName = serviceName.replace("ServiceImpl", "").replace("Service", "");
        
        // Determine Action
        String action = "ACTION";
        if (methodName.startsWith("create")) {
            action = "CREATE";
        } else if (methodName.startsWith("update")) {
            action = "UPDATE";
        } else if (methodName.startsWith("delete")) {
            action = "DELETE";
        }

        try {
            Object result = joinPoint.proceed();
            
            // Try to extract ID for logging
            String idVal = "UNKNOWN";
            if ("DELETE".equals(action)) {
                // For delete, usually the first parameter is the ID
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    idVal = String.valueOf(args[0]);
                }
            } else {
                // For create/update, try to get ID from returned object (DTO or Entity)
                if (result != null) {
                    idVal = getObjectId(result, entityName);
                }
            }

            log.info("[{}] {} - id={}", action, entityName, idVal);
            return result;
        } catch (Exception ex) {
            log.error("[{}] {} failed: {}", action, entityName, ex.getMessage());
            throw ex;
        }
    }

    private String getObjectId(Object obj, String entityName) {
        // Try getters like getEmployeeId(), getProjectId(), getAllocationId(), getId()
        String[] methodNames = {
            "get" + entityName + "Id",
            "getId"
        };
        for (String mName : methodNames) {
            try {
                Method method = obj.getClass().getMethod(mName);
                Object val = method.invoke(obj);
                if (val != null) {
                    return String.valueOf(val);
                }
            } catch (Exception ignored) {
            }
        }
        return "UNKNOWN";
    }
}
