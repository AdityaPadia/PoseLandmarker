package com.google.mediapipe.examples.poselandmarker;

import com.google.auto.value.AutoValue;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Landmark;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.TaskResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.List;
import java.util.Iterator;

@AutoValue
public abstract class CustomPoseLandmarkerResult implements TaskResult {
    public CustomPoseLandmarkerResult() {

    }

    static CustomPoseLandmarkerResult create(List<List<NormalizedLandmark>> landmarks, List<List<Landmark>> worldLandmarks, Optional<List<MPImage>> segmentationMasksData, long timestampMs) {
        Optional<List<MPImage>> multiPoseSegmentationMasks = Optional.empty();

        List<LandmarkProto.NormalizedLandmarkList> landmarksProto = new ArrayList<>();

        for (List<NormalizedLandmark> poseLandmarks : landmarks) {
            LandmarkProto.NormalizedLandmarkList.Builder poseLandmarksBuilder =
                    LandmarkProto.NormalizedLandmarkList.newBuilder();

            for (NormalizedLandmark normalizedLandmark : poseLandmarks) {
                LandmarkProto.NormalizedLandmark.Builder landmarkBuilder =
                        LandmarkProto.NormalizedLandmark.newBuilder()
                                .setX(normalizedLandmark.x())
                                .setY(normalizedLandmark.y())
                                .setZ(normalizedLandmark.z());

                // Add individual landmark to the list
                poseLandmarksBuilder.addLandmark(landmarkBuilder.build());
            }

            // Add the pose landmarks to the landmarksProto list
            landmarksProto.add(poseLandmarksBuilder.build());
        }

        List<LandmarkProto.LandmarkList> worldLandmarksProto = new ArrayList<>();
        for (List<Landmark> poseWorldLandmarks : worldLandmarks) {
            LandmarkProto.LandmarkList.Builder poseWorldLandmarksBuilder =
                    LandmarkProto.LandmarkList.newBuilder();
            for (Landmark poseWorldLandmark : poseWorldLandmarks) {
                LandmarkProto.Landmark worldLandmarkProto =
                        LandmarkProto.Landmark.newBuilder()
                                .setX(poseWorldLandmark.x())
                                .setY(poseWorldLandmark.y())
                                .setZ(poseWorldLandmark.z())
                                .build();
                poseWorldLandmarksBuilder.addLandmark(worldLandmarkProto);
            }
            worldLandmarksProto.add(poseWorldLandmarksBuilder.build());
        }


        if (segmentationMasksData.isPresent()) {
            multiPoseSegmentationMasks = Optional.of(Collections.unmodifiableList(segmentationMasksData.get()));
        }


        return new AutoValue_CustomPoseLandmarkerResult(
                timestampMs,
                landmarks,
                worldLandmarks,
                segmentationMasksData);
    }

    public abstract long timestampMs();

    public abstract List<List<NormalizedLandmark>> landmarks();

    public abstract List<List<Landmark>> worldLandmarks();

    public abstract Optional<List<MPImage>> segmentationMasks();

}
