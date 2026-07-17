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
        
        String entityName = serviceName.replace("ServiceImpl", "").replace("Service", "");
        
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
            
            String idVal = "UNKNOWN";
            if ("DELETE".equals(action)) {
                
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    idVal = String.valueOf(args[0]);
                }
            } else {
                
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
