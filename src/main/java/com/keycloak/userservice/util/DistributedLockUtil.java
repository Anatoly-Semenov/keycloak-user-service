package com.keycloak.userservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockUtil {

    private final RedissonClient redissonClient;
    
    private static final long DEFAULT_WAIT_TIME = 10;
    private static final long DEFAULT_LEASE_TIME = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * Выполняет операцию внутри распределенной блокировки
     *
     * @param lockKey ключ блокировки
     * @param supplier поставщик результата операции
     * @param <T> тип результата
     * @return результат выполнения операции
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, DEFAULT_TIME_UNIT, supplier);
    }

    /**
     * Выполняет операцию внутри распределенной блокировки с пользовательскими параметрами
     *
     * @param lockKey ключ блокировки
     * @param waitTime время ожидания блокировки
     * @param leaseTime время удержания блокировки
     * @param timeUnit единица измерения времени
     * @param supplier поставщик результата операции
     * @param <T> тип результата
     * @return результат выполнения операции
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (locked) {
                log.debug("Acquired lock: {}", lockKey);
                return supplier.get();
            } else {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new RuntimeException("Failed to acquire distributed lock: " + lockKey);
            }
        } catch (InterruptedException e) {
            log.error("Lock acquisition interrupted for key: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted for key: " + lockKey, e);
        } finally {
            if (locked) {
                lock.unlock();
                log.debug("Released lock: {}", lockKey);
            }
        }
    }

    /**
     * Выполняет операцию без результата внутри распределенной блокировки
     *
     * @param lockKey ключ блокировки
     * @param runnable операция для выполнения
     */
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, () -> {
            runnable.run();
            return null;
        });
    }
} 