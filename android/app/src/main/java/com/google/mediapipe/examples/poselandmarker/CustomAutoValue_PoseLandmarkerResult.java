package com.google.mediapipe.examples.poselandmarker;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Landmark;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;
import java.util.Optional;

final class CustomAutoValue_PoseLandmarkerResult extends CustomPoseLandmarkerResult {
    private final long timestampMs;
    private final List<List<NormalizedLandmark>> landmarks;
    private final List<List<Landmark>> worldLandmarks;
    private final Optional<List<MPImage>> segmentationMasks;

    CustomAutoValue_PoseLandmarkerResult(long timestampMs, List<List<NormalizedLandmark>> landmarks, List<List<Landmark>> worldLandmarks, Optional<List<MPImage>> segmentationMasks) {
        this.timestampMs = timestampMs;
        if (landmarks == null) {
            throw new NullPointerException("Null landmarks");
        } else {
            this.landmarks = landmarks;
            if (worldLandmarks == null) {
                throw new NullPointerException("Null worldLandmarks");
            } else {
                this.worldLandmarks = worldLandmarks;
                if (segmentationMasks == null) {
                    throw new NullPointerException("Null segmentationMasks");
                } else {
                    this.segmentationMasks = segmentationMasks;
                }
            }
        }
    }

    public long timestampMs() {
        return this.timestampMs;
    }

    public List<List<NormalizedLandmark>> landmarks() {
        return this.landmarks;
    }

    public List<List<Landmark>> worldLandmarks() {
        return this.worldLandmarks;
    }

    public Optional<List<MPImage>> segmentationMasks() {
        return this.segmentationMasks;
    }

    public String toString() {
        return "PoseLandmarkerResult{timestampMs=" + this.timestampMs + ", landmarks=" + this.landmarks + ", worldLandmarks=" + this.worldLandmarks + ", segmentationMasks=" + this.segmentationMasks + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PoseLandmarkerResult)) {
            return false;
        } else {
            PoseLandmarkerResult that = (PoseLandmarkerResult)o;
            return this.timestampMs == that.timestampMs() && this.landmarks.equals(that.landmarks()) && this.worldLandmarks.equals(that.worldLandmarks()) && this.segmentationMasks.equals(that.segmentationMasks());
        }
    }

    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (int)(this.timestampMs >>> 32 ^ this.timestampMs);
        h$ *= 1000003;
        h$ ^= this.landmarks.hashCode();
        h$ *= 1000003;
        h$ ^= this.worldLandmarks.hashCode();
        h$ *= 1000003;
        h$ ^= this.segmentationMasks.hashCode();
        return h$;
    }
}

