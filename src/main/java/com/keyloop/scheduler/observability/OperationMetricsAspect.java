package com.keyloop.scheduler.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OperationMetricsAspect {

    private final MeterRegistry meterRegistry;

    public OperationMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(observedOperation)")
    public Object recordOperation(ProceedingJoinPoint joinPoint,
                                  ObservedOperation observedOperation) throws Throwable {
        long startedAt = System.nanoTime();
        String outcome = "success";
        try {
            return joinPoint.proceed();
        } catch (Throwable exception) {
            outcome = "error";
            throw exception;
        } finally {
            Timer.builder("scheduler.application.operation")
                    .description("Application operation execution time")
                    .tag("operation", observedOperation.value())
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        }
    }
}
