package com.game.web;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.util.Log;

public class AndroidBug5497Workaround {

    // For more information, see https://code.google.com/p/android/issues/detail?id=5497
    // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.
	
	private static final String TAG = "SubWebView";

    private View mChildOfContent;
    private int mUsableHeightPrevious;
    private FrameLayout.LayoutParams mFrameLayoutParams;
	private ViewTreeObserver.OnGlobalLayoutListener mListener;
	private boolean mAttach = false;
	private int mOldHeight;
	MainActivity mMainActivity;

    public AndroidBug5497Workaround(MainActivity activity, View view) {
        mMainActivity = activity;
        mChildOfContent = view;

		mListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                possiblyResizeChildOfContent();
            }
        };
        mFrameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();
    }
	
	public void attach()
	{
		if (mAttach == false)
		{
			Log.d(TAG, "attach");
			mAttach = true;
			mOldHeight = mFrameLayoutParams.height;
			mUsableHeightPrevious = 0;
			mChildOfContent.requestLayout();
			mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(mListener);
		}
	}
	
	public void detach()
	{
		if (mAttach)
		{
			Log.d(TAG, "detach");
			mAttach = false;
			mFrameLayoutParams.height = mOldHeight;
			mChildOfContent.getViewTreeObserver().removeGlobalOnLayoutListener(mListener);
		}
	}

    private void possiblyResizeChildOfContent() {
        int usableHeightNow = computeUsableHeight();
        if (usableHeightNow != mUsableHeightPrevious) {
            int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;

            if (heightDifference > (usableHeightSansKeyboard/4)) {
                // keyboard probably just became visible
                mFrameLayoutParams.height = usableHeightSansKeyboard - heightDifference;
            } else {
                // keyboard probably just became hidden
                mFrameLayoutParams.height = usableHeightSansKeyboard;
            }
            mChildOfContent.requestLayout();
            mUsableHeightPrevious = usableHeightNow;
        }
    }

    private int computeUsableHeight() {
        Rect r = new Rect();
        mChildOfContent.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);//  return r.bottom
    }
}