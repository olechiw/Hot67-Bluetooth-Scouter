package org.hotteam67.common;

/**
 * Interface for download results, with two conditions: failure and completion
 *
 * @param <Result>
 */
public interface OnDownloadResultListener<Result>
{

    void onComplete(Result result);

    void onFail();
}
