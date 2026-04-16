package com.dji.sample.component.mqtt.service;

import com.dji.sdk.mqtt.IMqttMessageGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Pull one JPEG frame from RTMP periodically and publish it to MQTT.
 * <p>
 * Requires {@code ffmpeg} on PATH. Payload is raw JPEG bytes (not Base64).
 */
@Component
@Slf4j
public class RtmpFrameMqttPublisher {

    private static final long DEFAULT_FFMPEG_TIMEOUT_MS = 4000L;

    /** Avoid blocking the scheduler thread indefinitely if ffmpeg never closes stdout. */
    private static final ExecutorService FFMPEG_STDOUT_READER = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "silas-rtmp-ffmpeg-read");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    private IMqttMessageGateway mqttMessageGateway;

    @Value("${silas.live.image-publisher.enabled:false}")
    private boolean enabled;

    @Value("${silas.live.image-publisher.rtmp-url:}")
    private String rtmpUrl;

    @Value("${silas.live.image-publisher.topic:silas/live/image}")
    private String topic;

    @Value("${silas.live.image-publisher.qos:0}")
    private int qos;

    @Value("${silas.live.image-publisher.ffmpeg-timeout-ms:4000}")
    private long ffmpegTimeoutMs;

    private volatile boolean configWarned;

    @Scheduled(
            initialDelayString = "${silas.live.image-publisher.initial-delay-ms:5000}",
            fixedDelayString = "${silas.live.image-publisher.interval-ms:1000}")
    public void pullFrameAndPublish() {
        if (!enabled) {
            return;
        }

        if (!StringUtils.hasText(rtmpUrl) || !StringUtils.hasText(topic)) {
            if (!configWarned) {
                log.warn("silas.live.image-publisher is enabled, but rtmp-url/topic is empty. Skipping.");
                configWarned = true;
            }
            return;
        }

        byte[] image = captureSingleJpegFrame(rtmpUrl);
        if (image == null || image.length == 0) {
            return;
        }

        try {
            mqttMessageGateway.publish(topic, image, qos);
            log.debug("RtmpFrameMqttPublisher: published topic={} qos={} bytes={}", topic, qos, image.length);
        } catch (Exception ex) {
            log.error(
                    "RtmpFrameMqttPublisher.pullFrameAndPublish -> IMqttMessageGateway.publish(topic={}, qos={}, bytes={}) failed: {}",
                    topic,
                    qos,
                    image.length,
                    ex.toString(),
                    ex);
        }
    }

    private byte[] captureSingleJpegFrame(String url) {
        Process process = null;
        Future<byte[]> stdoutFuture = null;
        long timeout = ffmpegTimeoutMs > 0 ? ffmpegTimeoutMs : DEFAULT_FFMPEG_TIMEOUT_MS;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-hide_banner",
                    "-loglevel", "error",
                    "-i", url,
                    "-frames:v", "1",
                    "-f", "image2pipe",
                    "-vcodec", "mjpeg",
                    "-"
            );
            pb.redirectErrorStream(true);
            process = pb.start();
            final Process p = process;
            stdoutFuture = FFMPEG_STDOUT_READER.submit(() -> p.getInputStream().readAllBytes());

            byte[] data;
            try {
                data = stdoutFuture.get(timeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                stdoutFuture.cancel(true);
                log.warn("ffmpeg stdout read timed out after {} ms, url={}", timeout, url);
                return null;
            } catch (ExecutionException e) {
                log.warn("ffmpeg stdout read failed, url={}", url, e.getCause());
                return null;
            }

            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("ffmpeg did not exit after stdout closed, url={}", url);
                return null;
            }
            if (process.exitValue() != 0) {
                log.warn("ffmpeg exited with code {} while pulling frame from {}", process.exitValue(), url);
                return null;
            }
            if (data.length == 0) {
                log.warn("ffmpeg returned empty frame from {}", url);
                return null;
            }
            return data;
        } catch (IOException e) {
            log.error("Cannot run ffmpeg. Ensure ffmpeg is installed and in PATH.", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for ffmpeg frame capture");
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while capturing frame from {}", url, e);
            return null;
        } finally {
            if (stdoutFuture != null && !stdoutFuture.isDone()) {
                stdoutFuture.cancel(true);
            }
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
