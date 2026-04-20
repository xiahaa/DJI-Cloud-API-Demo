package com.dji.sample.control.service.impl;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wait/notify bridge for one-shot OSD fetch by target serial number.
 */
@Component
public class RcPlus2DrcOsdWaitService {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<CompletableFuture<Object>>> waiters = new ConcurrentHashMap<>();

    public Object awaitNext(String keySn, long timeoutMs) throws TimeoutException {
        CompletableFuture<Object> future = new CompletableFuture<>();
        waiters.computeIfAbsent(keySn, key -> new CopyOnWriteArrayList<>()).add(future);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting OSD message.", ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException("Failed while waiting OSD message.", ex.getCause());
        } finally {
            List<CompletableFuture<Object>> futures = waiters.get(keySn);
            if (futures != null) {
                futures.remove(future);
                if (futures.isEmpty()) {
                    waiters.remove(keySn, (CopyOnWriteArrayList<CompletableFuture<Object>>) futures);
                }
            }
        }
    }

    public void publish(String keySn, Object osdData) {
        List<CompletableFuture<Object>> futures = waiters.remove(keySn);
        if (futures == null || futures.isEmpty()) {
            return;
        }
        futures.forEach(future -> future.complete(osdData));
    }
}
