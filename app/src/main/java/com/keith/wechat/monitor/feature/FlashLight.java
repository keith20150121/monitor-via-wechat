package com.keith.wechat.monitor.feature;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

public class FlashLight {
    private static final String TAG = "no-man-FlashLight";

    private final boolean mExist;
    private String mCameraId;
    private CameraManager mCameraManager;

    public FlashLight(Context context) {
        mExist = context.getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!mExist) {
            return;
        }
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        tryInitCamera();
    }

    private String getCameraId() throws CameraAccessException {
        String[] ids = mCameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable
                    && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }

    private void tryInitCamera() {
        try {
            mCameraId = getCameraId();
        } catch (Throwable e) {
            Log.e(TAG, "Couldn't initialize.", e);
        }
    }

    public boolean setFlashlight(boolean enabled) {
        if (!mExist) {
            Log.e(TAG, "flashlight doesn't exist.");
            return false;
        }
        try {
            mCameraManager.setTorchMode(mCameraId, enabled);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Couldn't set torch mode", e);
            return false;
        }
        return true;
    }
}
