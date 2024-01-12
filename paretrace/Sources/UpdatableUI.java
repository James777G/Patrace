package com.arm.pa.paretrace.Sources;

public interface UpdatableUI {

    void update();

    void updateProgress(int progress, float fileLength);

    void updateFailureMessage();
}
