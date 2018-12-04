package org.hotteam67.common;

public interface OnDownloadResultListener<Result> {

    void onComplete(Result result);
    void onFail();
}
