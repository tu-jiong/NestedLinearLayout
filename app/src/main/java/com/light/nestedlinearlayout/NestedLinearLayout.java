package com.light.nestedlinearlayout;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.FocusFinder;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.widget.EdgeEffect;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import java.util.List;

/**
 * Created by Tujiong on 2018/3/6.
 */
public class NestedLinearLayout extends LinearLayout implements NestedScrollingParent, NestedScrollingChild2, ScrollingView {

    private static final String TAG = "NestedLinearLayout";
    private long mLastScroll;
    private final Rect mTempRect;
    private OverScroller mScroller;
    private EdgeEffect mEdgeGlowTop;
    private EdgeEffect mEdgeGlowBottom;
    private int mLastMotionY;
    private boolean mIsLayoutDirty;
    private boolean mIsLaidOut;
    private View mChildToScrollTo;
    private boolean mIsBeingDragged;
    private VelocityTracker mVelocityTracker;
    private boolean mFillViewport;
    private boolean mSmoothScrollingEnabled;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mActivePointerId;
    private final int[] mScrollOffset;
    private final int[] mScrollConsumed;
    private int mNestedYOffset;
    private int mLastScrollerY;
    private NestedLinearLayout.SavedState mSavedState;
    private static final NestedLinearLayout.AccessibilityDelegate ACCESSIBILITY_DELEGATE = new NestedLinearLayout.AccessibilityDelegate();
    private final NestedScrollingParentHelper mParentHelper;
    private final NestedScrollingChildHelper mChildHelper;
    private float mVerticalScrollFactor;
    private NestedLinearLayout.OnScrollChangeListener mOnScrollChangeListener;

    public NestedLinearLayout(@NonNull Context context) {
        this(context, null);
    }

    public NestedLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mTempRect = new Rect();
        this.mIsLayoutDirty = true;
        this.mIsLaidOut = false;
        this.mChildToScrollTo = null;
        this.mIsBeingDragged = false;
        this.mSmoothScrollingEnabled = true;
        this.mActivePointerId = -1;
        this.mScrollOffset = new int[2];
        this.mScrollConsumed = new int[2];
        this.initScrollView();
        this.mParentHelper = new NestedScrollingParentHelper(this);
        this.mChildHelper = new NestedScrollingChildHelper(this);
        this.setNestedScrollingEnabled(true);
        ViewCompat.setAccessibilityDelegate(this, ACCESSIBILITY_DELEGATE);
    }

    public void setNestedScrollingEnabled(boolean enabled) {
        this.mChildHelper.setNestedScrollingEnabled(enabled);
    }

    public boolean isNestedScrollingEnabled() {
        return this.mChildHelper.isNestedScrollingEnabled();
    }

    public boolean startNestedScroll(int axes) {
        return this.mChildHelper.startNestedScroll(axes);
    }

    public boolean startNestedScroll(int axes, int type) {
        return this.mChildHelper.startNestedScroll(axes, type);
    }

    public void stopNestedScroll() {
        this.mChildHelper.stopNestedScroll();
    }

    public void stopNestedScroll(int type) {
        this.mChildHelper.stopNestedScroll(type);
    }

    public boolean hasNestedScrollingParent() {
        return this.mChildHelper.hasNestedScrollingParent();
    }

    public boolean hasNestedScrollingParent(int type) {
        return this.mChildHelper.hasNestedScrollingParent(type);
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return this.mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow, int type) {
        return this.mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return this.mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        return this.mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return this.mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return this.mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & 2) != 0;
    }

    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        this.mParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        this.startNestedScroll(2);
    }

    public void onStopNestedScroll(View target) {
        this.mParentHelper.onStopNestedScroll(target);
        this.stopNestedScroll();
    }

    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        int oldScrollY = this.getScrollY();
        this.scrollBy(0, dyUnconsumed);
        int myConsumed = this.getScrollY() - oldScrollY;
        int myUnconsumed = dyUnconsumed - myConsumed;
        this.dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null);
    }

    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        this.dispatchNestedPreScroll(dx, dy, consumed, null);
    }

    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if (!consumed) {
            this.flingWithNestedDispatch((int) velocityY);
            return true;
        } else {
            return false;
        }
    }

    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return this.dispatchNestedPreFling(velocityX, velocityY);
    }

    public int getNestedScrollAxes() {
        return this.mParentHelper.getNestedScrollAxes();
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    protected float getTopFadingEdgeStrength() {
        if (this.getChildCount() == 0) {
            return 0.0F;
        } else {
            int length = this.getVerticalFadingEdgeLength();
            int scrollY = this.getScrollY();
            return scrollY < length ? (float) scrollY / (float) length : 1.0F;
        }
    }

    protected float getBottomFadingEdgeStrength() {
        if (this.getChildCount() == 0) {
            return 0.0F;
        } else {
            int length = this.getVerticalFadingEdgeLength();
            int bottomEdge = this.getHeight() - this.getPaddingBottom();
            int span = this.getChildAt(0).getBottom() - this.getScrollY() - bottomEdge;
            return span < length ? (float) span / (float) length : 1.0F;
        }
    }

    public int getMaxScrollAmount() {
        return (int) (0.5F * (float) this.getHeight());
    }

    private void initScrollView() {
        this.mScroller = new OverScroller(this.getContext());
        this.setFocusable(true);
        this.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        this.setWillNotDraw(false);
        ViewConfiguration configuration = ViewConfiguration.get(this.getContext());
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public void setOnScrollChangeListener(@Nullable NestedLinearLayout.OnScrollChangeListener l) {
        this.mOnScrollChangeListener = l;
    }

    private boolean canScroll() {
        View child = this.getChildAt(0);
        if (child != null) {
            int childHeight = child.getHeight();
            return this.getHeight() < childHeight + this.getPaddingTop() + this.getPaddingBottom();
        } else {
            return false;
        }
    }

    public boolean isFillViewport() {
        return this.mFillViewport;
    }

    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != this.mFillViewport) {
            this.mFillViewport = fillViewport;
            this.requestLayout();
        }
    }

    public boolean isSmoothScrollingEnabled() {
        return this.mSmoothScrollingEnabled;
    }

    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        this.mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (this.mOnScrollChangeListener != null) {
            this.mOnScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
        }

    }

    private boolean inChild(int x, int y) {
        if (this.getChildCount() <= 0) {
            return false;
        } else {
            int scrollY = this.getScrollY();
            View child = this.getChildAt(0);
            return y >= child.getTop() - scrollY && y < child.getBottom() - scrollY && x >= child.getLeft() && x < child.getRight();
        }
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }

    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }

    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }

    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            this.recycleVelocityTracker();
        }

        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_MOVE && this.mIsBeingDragged) {
            return true;
        } else {
            int activePointerId;
            switch (action & 255) {
                case MotionEvent.ACTION_DOWN:
                    activePointerId = (int) ev.getY();
                    if (!this.inChild((int) ev.getX(), activePointerId)) {
                        this.mIsBeingDragged = false;
                        this.recycleVelocityTracker();
                    } else {
                        this.mLastMotionY = activePointerId;
                        this.mActivePointerId = ev.getPointerId(0);
                        this.initOrResetVelocityTracker();
                        this.mVelocityTracker.addMovement(ev);
                        this.mScroller.computeScrollOffset();
                        this.mIsBeingDragged = !this.mScroller.isFinished();
                        this.startNestedScroll(2, 0);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    this.mIsBeingDragged = false;
                    this.mActivePointerId = -1;
                    this.recycleVelocityTracker();
                    if (this.mScroller.springBack(this.getScrollX(), this.getScrollY(), 0, 0, 0, this.getScrollRange())) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }

                    this.stopNestedScroll(0);
                    break;
                case MotionEvent.ACTION_MOVE:
                    activePointerId = this.mActivePointerId;
                    if (activePointerId != -1) {
                        int pointerIndex = ev.findPointerIndex(activePointerId);
                        if (pointerIndex == -1) {
                            Log.e("NestedLinearLayout", "Invalid pointerId=" + activePointerId + " in onInterceptTouchEvent");
                        } else {
                            int y = (int) ev.getY(pointerIndex);
                            int yDiff = Math.abs(y - this.mLastMotionY);
                            if (yDiff > this.mTouchSlop && (this.getNestedScrollAxes() & 2) == 0) {
                                this.mIsBeingDragged = true;
                                this.mLastMotionY = y;
                                this.initVelocityTrackerIfNotExists();
                                this.mVelocityTracker.addMovement(ev);
                                this.mNestedYOffset = 0;
                                ViewParent parent = this.getParent();
                                if (parent != null) {
                                    parent.requestDisallowInterceptTouchEvent(true);
                                }
                            }
                        }
                    }
            }

            return this.mIsBeingDragged;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        this.initVelocityTrackerIfNotExists();
        MotionEvent vtev = MotionEvent.obtain(ev);
        int actionMasked = ev.getActionMasked();
        if (actionMasked == 0) {
            this.mNestedYOffset = 0;
        }

        vtev.offsetLocation(0.0F, (float) this.mNestedYOffset);
        int range;
        int overscrollMode;
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                if (this.getChildCount() == 0) {
                    return false;
                }

                if (this.mIsBeingDragged = !this.mScroller.isFinished()) {
                    ViewParent parent = this.getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                if (!this.mScroller.isFinished()) {
                    this.mScroller.abortAnimation();
                }

                this.mLastMotionY = (int) ev.getY();
                this.mActivePointerId = ev.getPointerId(0);
                this.startNestedScroll(2, 0);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                VelocityTracker velocityTracker = this.mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                range = (int) velocityTracker.getYVelocity(this.mActivePointerId);
                if (Math.abs(range) > this.mMinimumVelocity) {
                    this.flingWithNestedDispatch(-range);
                } else if (this.mScroller.springBack(this.getScrollX(), this.getScrollY(), 0, 0, 0, this.getScrollRange())) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }

                this.mActivePointerId = -1;
                this.endDrag();
                break;
            case MotionEvent.ACTION_MOVE:
                int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e("NestedLinearLayout", "Invalid pointerId=" + this.mActivePointerId + " in onTouchEvent");
                } else {
                    int y = (int) ev.getY(activePointerIndex);
                    int deltaY = this.mLastMotionY - y;
                    if (this.dispatchNestedPreScroll(0, deltaY, this.mScrollConsumed, this.mScrollOffset, 0)) {
                        deltaY -= this.mScrollConsumed[1];
                        vtev.offsetLocation(0.0F, (float) this.mScrollOffset[1]);
                        this.mNestedYOffset += this.mScrollOffset[1];
                    }

                    if (!this.mIsBeingDragged && Math.abs(deltaY) > this.mTouchSlop) {
                        ViewParent parent = this.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }

                        this.mIsBeingDragged = true;
                        if (deltaY > 0) {
                            deltaY -= this.mTouchSlop;
                        } else {
                            deltaY += this.mTouchSlop;
                        }
                    }

                    if (this.mIsBeingDragged) {
                        this.mLastMotionY = y - this.mScrollOffset[1];
                        int oldY = this.getScrollY();
                        range = this.getScrollRange();
                        overscrollMode = this.getOverScrollMode();
                        boolean canOverscroll = overscrollMode == 0 || overscrollMode == 1 && range > 0;
                        if (this.overScrollByCompat(0, deltaY, 0, this.getScrollY(), 0, range, 0, 0, true) && !this.hasNestedScrollingParent(0)) {
                            this.mVelocityTracker.clear();
                        }

                        int scrolledDeltaY = this.getScrollY() - oldY;
                        int unconsumedY = deltaY - scrolledDeltaY;
                        if (this.dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, this.mScrollOffset, 0)) {
                            this.mLastMotionY -= this.mScrollOffset[1];
                            vtev.offsetLocation(0.0F, (float) this.mScrollOffset[1]);
                            this.mNestedYOffset += this.mScrollOffset[1];
                        } else if (canOverscroll) {
                            this.ensureGlows();
                            int pulledToY = oldY + deltaY;
                            if (pulledToY < 0) {
                                EdgeEffectCompat.onPull(this.mEdgeGlowTop, (float) deltaY / (float) this.getHeight(), ev.getX(activePointerIndex) / (float) this.getWidth());
                                if (!this.mEdgeGlowBottom.isFinished()) {
                                    this.mEdgeGlowBottom.onRelease();
                                }
                            } else if (pulledToY > range) {
                                EdgeEffectCompat.onPull(this.mEdgeGlowBottom, (float) deltaY / (float) this.getHeight(), 1.0F - ev.getX(activePointerIndex) / (float) this.getWidth());
                                if (!this.mEdgeGlowTop.isFinished()) {
                                    this.mEdgeGlowTop.onRelease();
                                }
                            }

                            if (this.mEdgeGlowTop != null && (!this.mEdgeGlowTop.isFinished() || !this.mEdgeGlowBottom.isFinished())) {
                                ViewCompat.postInvalidateOnAnimation(this);
                            }
                        }
                    }
                }
                break;
        }

        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(vtev);
        }

        vtev.recycle();
        return true;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & 2) != 0) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL:
                    if (!this.mIsBeingDragged) {
                        float vscroll = event.getAxisValue(9);
                        if (vscroll != 0.0F) {
                            int delta = (int) (vscroll * this.getVerticalScrollFactorCompat());
                            int range = this.getScrollRange();
                            int oldScrollY = this.getScrollY();
                            int newScrollY = oldScrollY - delta;
                            if (newScrollY < 0) {
                                newScrollY = 0;
                            } else if (newScrollY > range) {
                                newScrollY = range;
                            }

                            if (newScrollY != oldScrollY) {
                                super.scrollTo(this.getScrollX(), newScrollY);
                                return true;
                            }
                        }
                    }
            }
        }

        return false;
    }

    private float getVerticalScrollFactorCompat() {
        if (this.mVerticalScrollFactor == 0.0F) {
            TypedValue outValue = new TypedValue();
            Context context = this.getContext();
            if (!context.getTheme().resolveAttribute(16842829, outValue, true)) {
                throw new IllegalStateException("Expected theme to define listPreferredItemHeight.");
            }

            this.mVerticalScrollFactor = outValue.getDimension(context.getResources().getDisplayMetrics());
        }

        return this.mVerticalScrollFactor;
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX, scrollY);
    }

    boolean overScrollByCompat(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        int overScrollMode = this.getOverScrollMode();
        boolean canScrollHorizontal = this.computeHorizontalScrollRange() > this.computeHorizontalScrollExtent();
        boolean canScrollVertical = this.computeVerticalScrollRange() > this.computeVerticalScrollExtent();
        boolean overScrollHorizontal = overScrollMode == 0 || overScrollMode == 1 && canScrollHorizontal;
        boolean overScrollVertical = overScrollMode == 0 || overScrollMode == 1 && canScrollVertical;
        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        int left = -maxOverScrollX;
        int right = maxOverScrollX + scrollRangeX;
        int top = -maxOverScrollY;
        int bottom = maxOverScrollY + scrollRangeY;
        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        if (clampedY && !this.hasNestedScrollingParent(1)) {
            this.mScroller.springBack(newScrollX, newScrollY, 0, 0, 0, this.getScrollRange());
        }

        this.onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);
        return clampedX || clampedY;
    }

    int getScrollRange() {
        int scrollRange = 0;
        if (this.getChildCount() > 0) {
            View child = this.getChildAt(0);
            scrollRange = Math.max(0, child.getHeight() - (this.getHeight() - this.getPaddingBottom() - this.getPaddingTop()));
        }

        return scrollRange;
    }

    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {
        List<View> focusables = this.getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;
        boolean foundFullyContainedFocusable = false;
        int count = focusables.size();

        for (int i = 0; i < count; ++i) {
            View view = focusables.get(i);
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();
            if (top < viewBottom && viewTop < bottom) {
                boolean viewIsFullyContained = top < viewTop && viewBottom < bottom;
                if (focusCandidate == null) {
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    boolean viewIsCloserToBoundary = topFocus && viewTop < focusCandidate.getTop() || !topFocus && viewBottom > focusCandidate.getBottom();
                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            focusCandidate = view;
                        }
                    } else if (viewIsFullyContained) {
                        focusCandidate = view;
                        foundFullyContainedFocusable = true;
                    } else if (viewIsCloserToBoundary) {
                        focusCandidate = view;
                    }
                }
            }
        }

        return focusCandidate;
    }

    public boolean pageScroll(int direction) {
        boolean down = direction == 130;
        int height = this.getHeight();
        if (down) {
            this.mTempRect.top = this.getScrollY() + height;
            int count = this.getChildCount();
            if (count > 0) {
                View view = this.getChildAt(count - 1);
                if (this.mTempRect.top + height > view.getBottom()) {
                    this.mTempRect.top = view.getBottom() - height;
                }
            }
        } else {
            this.mTempRect.top = this.getScrollY() - height;
            if (this.mTempRect.top < 0) {
                this.mTempRect.top = 0;
            }
        }

        this.mTempRect.bottom = this.mTempRect.top + height;
        return this.scrollAndFocus(direction, this.mTempRect.top, this.mTempRect.bottom);
    }

    public boolean fullScroll(int direction) {
        boolean down = direction == 130;
        int height = this.getHeight();
        this.mTempRect.top = 0;
        this.mTempRect.bottom = height;
        if (down) {
            int count = this.getChildCount();
            if (count > 0) {
                View view = this.getChildAt(count - 1);
                this.mTempRect.bottom = view.getBottom() + this.getPaddingBottom();
                this.mTempRect.top = this.mTempRect.bottom - height;
            }
        }

        return this.scrollAndFocus(direction, this.mTempRect.top, this.mTempRect.bottom);
    }

    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;
        int height = this.getHeight();
        int containerTop = this.getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == 33;
        View newFocused = this.findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }

        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int delta = up ? top - containerTop : bottom - containerBottom;
            this.doScrollY(delta);
        }

        if (newFocused != this.findFocus()) {
            newFocused.requestFocus(direction);
        }

        return handled;
    }

    public boolean arrowScroll(int direction) {
        View currentFocused = this.findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        }

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
        int maxJump = this.getMaxScrollAmount();
        int scrollDelta;
        if (nextFocused != null && this.isWithinDeltaOfScreen(nextFocused, maxJump, this.getHeight())) {
            nextFocused.getDrawingRect(this.mTempRect);
            this.offsetDescendantRectToMyCoords(nextFocused, this.mTempRect);
            scrollDelta = this.computeScrollDeltaToGetChildRectOnScreen(this.mTempRect);
            this.doScrollY(scrollDelta);
            nextFocused.requestFocus(direction);
        } else {
            scrollDelta = maxJump;
            if (direction == 33 && this.getScrollY() < maxJump) {
                scrollDelta = this.getScrollY();
            } else if (direction == 130 && this.getChildCount() > 0) {
                int daBottom = this.getChildAt(0).getBottom();
                int screenBottom = this.getScrollY() + this.getHeight() - this.getPaddingBottom();
                if (daBottom - screenBottom < maxJump) {
                    scrollDelta = daBottom - screenBottom;
                }
            }

            if (scrollDelta == 0) {
                return false;
            }

            this.doScrollY(direction == 130 ? scrollDelta : -scrollDelta);
        }

        if (currentFocused != null && currentFocused.isFocused() && this.isOffScreen(currentFocused)) {
            scrollDelta = this.getDescendantFocusability();
            this.setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
            this.requestFocus();
            this.setDescendantFocusability(scrollDelta);
        }

        return true;
    }

    private boolean isOffScreen(View descendant) {
        return !this.isWithinDeltaOfScreen(descendant, 0, this.getHeight());
    }

    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(this.mTempRect);
        this.offsetDescendantRectToMyCoords(descendant, this.mTempRect);
        return this.mTempRect.bottom + delta >= this.getScrollY() && this.mTempRect.top - delta <= this.getScrollY() + height;
    }

    private void doScrollY(int delta) {
        if (delta != 0) {
            if (this.mSmoothScrollingEnabled) {
                this.smoothScrollBy(0, delta);
            } else {
                this.scrollBy(0, delta);
            }
        }

    }

    public final void smoothScrollBy(int dx, int dy) {
        if (this.getChildCount() != 0) {
            long duration = AnimationUtils.currentAnimationTimeMillis() - this.mLastScroll;
            if (duration > 250L) {
                int height = this.getHeight() - this.getPaddingBottom() - this.getPaddingTop();
                int bottom = this.getChildAt(0).getHeight();
                int maxY = Math.max(0, bottom - height);
                int scrollY = this.getScrollY();
                dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
                this.mScroller.startScroll(this.getScrollX(), scrollY, 0, dy);
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                if (!this.mScroller.isFinished()) {
                    this.mScroller.abortAnimation();
                }

                this.scrollBy(dx, dy);
            }

            this.mLastScroll = AnimationUtils.currentAnimationTimeMillis();
        }
    }

    public final void smoothScrollTo(int x, int y) {
        this.smoothScrollBy(x - this.getScrollX(), y - this.getScrollY());
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int computeVerticalScrollRange() {
        int count = this.getChildCount();
        int contentHeight = this.getHeight() - this.getPaddingBottom() - this.getPaddingTop();
        if (count == 0) {
            return contentHeight;
        } else {
            int scrollRange = this.getChildAt(0).getBottom();
            int scrollY = this.getScrollY();
            int overscrollBottom = Math.max(0, scrollRange - contentHeight);
            if (scrollY < 0) {
                scrollRange -= scrollY;
            } else if (scrollY > overscrollBottom) {
                scrollRange += scrollY - overscrollBottom;
            }

            return scrollRange;
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int x = this.mScroller.getCurrX();
            int y = this.mScroller.getCurrY();
            int dy = y - this.mLastScrollerY;
            if (this.dispatchNestedPreScroll(0, dy, this.mScrollConsumed, null, 1)) {
                dy -= this.mScrollConsumed[1];
            }

            if (dy != 0) {
                int range = this.getScrollRange();
                int oldScrollY = this.getScrollY();
                this.overScrollByCompat(0, dy, this.getScrollX(), oldScrollY, 0, range, 0, 0, false);
                int scrolledDeltaY = this.getScrollY() - oldScrollY;
                int unconsumedY = dy - scrolledDeltaY;
                if (!this.dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, null, 1)) {
                    int mode = this.getOverScrollMode();
                    boolean canOverscroll = mode == 0 || mode == 1 && range > 0;
                    if (canOverscroll) {
                        this.ensureGlows();
                        if (y <= 0 && oldScrollY > 0) {
                            this.mEdgeGlowTop.onAbsorb((int) this.mScroller.getCurrVelocity());
                        } else if (y >= range && oldScrollY < range) {
                            this.mEdgeGlowBottom.onAbsorb((int) this.mScroller.getCurrVelocity());
                        }
                    }
                }
            }

            this.mLastScrollerY = y;
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (this.hasNestedScrollingParent(1)) {
                this.stopNestedScroll(1);
            }

            this.mLastScrollerY = 0;
        }

    }

    private void scrollToChild(View child) {
        child.getDrawingRect(this.mTempRect);
        this.offsetDescendantRectToMyCoords(child, this.mTempRect);
        int scrollDelta = this.computeScrollDeltaToGetChildRectOnScreen(this.mTempRect);
        if (scrollDelta != 0) {
            this.scrollBy(0, scrollDelta);
        }

    }

    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        int delta = this.computeScrollDeltaToGetChildRectOnScreen(rect);
        boolean scroll = delta != 0;
        if (scroll) {
            if (immediate) {
                this.scrollBy(0, delta);
            } else {
                this.smoothScrollBy(0, delta);
            }
        }

        return scroll;
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (this.getChildCount() == 0) {
            return 0;
        } else {
            int height = this.getHeight();
            int screenTop = this.getScrollY();
            int screenBottom = screenTop + height;
            int fadingEdge = this.getVerticalFadingEdgeLength();
            if (rect.top > 0) {
                screenTop += fadingEdge;
            }

            if (rect.bottom < this.getChildAt(0).getHeight()) {
                screenBottom -= fadingEdge;
            }

            int scrollYDelta = 0;
            if (rect.bottom > screenBottom && rect.top > screenTop) {
                if (rect.height() > height) {
                    scrollYDelta += rect.top - screenTop;
                } else {
                    scrollYDelta += rect.bottom - screenBottom;
                }

                int bottom = this.getChildAt(0).getBottom();
                int distanceToBottom = bottom - screenBottom;
                scrollYDelta = Math.min(scrollYDelta, distanceToBottom);
            } else if (rect.top < screenTop && rect.bottom < screenBottom) {
                if (rect.height() > height) {
                    scrollYDelta -= screenBottom - rect.bottom;
                } else {
                    scrollYDelta -= screenTop - rect.top;
                }

                scrollYDelta = Math.max(scrollYDelta, -this.getScrollY());
            }

            return scrollYDelta;
        }
    }

    public void requestChildFocus(View child, View focused) {
        if (!this.mIsLayoutDirty) {
            this.scrollToChild(focused);
        } else {
            this.mChildToScrollTo = focused;
        }

        super.requestChildFocus(child, focused);
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (direction == 2) {
            direction = 130;
        } else if (direction == 1) {
            direction = 33;
        }

        View nextFocus = previouslyFocusedRect == null ? FocusFinder.getInstance().findNextFocus(this, null, direction) : FocusFinder.getInstance().findNextFocusFromRect(this, previouslyFocusedRect, direction);
        return nextFocus != null && (!this.isOffScreen(nextFocus) && nextFocus.requestFocus(direction, previouslyFocusedRect));
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
        return this.scrollToChildRect(rectangle, immediate);
    }

    public void requestLayout() {
        this.mIsLayoutDirty = true;
        super.requestLayout();
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mIsLayoutDirty = false;
        if (this.mChildToScrollTo != null && isViewDescendantOf(this.mChildToScrollTo, this)) {
            this.scrollToChild(this.mChildToScrollTo);
        }

        this.mChildToScrollTo = null;
        if (!this.mIsLaidOut) {
            if (this.mSavedState != null) {
                this.scrollTo(this.getScrollX(), this.mSavedState.scrollPosition);
                this.mSavedState = null;
            }

            int childHeight = this.getChildCount() > 0 ? this.getChildAt(0).getMeasuredHeight() : 0;
            int scrollRange = Math.max(0, childHeight - (b - t - this.getPaddingBottom() - this.getPaddingTop()));
            if (this.getScrollY() > scrollRange) {
                this.scrollTo(this.getScrollX(), scrollRange);
            } else if (this.getScrollY() < 0) {
                this.scrollTo(this.getScrollX(), 0);
            }
        }

        this.scrollTo(this.getScrollX(), this.getScrollY());
        this.mIsLaidOut = true;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mIsLaidOut = false;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        View currentFocused = this.findFocus();
        if (null != currentFocused && this != currentFocused) {
            if (this.isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
                currentFocused.getDrawingRect(this.mTempRect);
                this.offsetDescendantRectToMyCoords(currentFocused, this.mTempRect);
                int scrollDelta = this.computeScrollDeltaToGetChildRectOnScreen(this.mTempRect);
                this.doScrollY(scrollDelta);
            }

        }
    }

    private static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        } else {
            ViewParent theParent = child.getParent();
            return theParent instanceof ViewGroup && isViewDescendantOf((View) theParent, parent);
        }
    }

    public void fling(int velocityY) {
        if (this.getChildCount() > 0) {
            this.startNestedScroll(2, 1);
            this.mScroller.fling(this.getScrollX(), this.getScrollY(), 0, velocityY, 0, 0, -2147483648, 2147483647, 0, 0);
            this.mLastScrollerY = this.getScrollY();
            ViewCompat.postInvalidateOnAnimation(this);
        }

    }

    private void flingWithNestedDispatch(int velocityY) {
        int scrollY = this.getScrollY();
        boolean canFling = (scrollY > 0 || velocityY > 0) && (scrollY < this.getScrollRange() || velocityY < 0);
        if (!this.dispatchNestedPreFling(0.0F, (float) velocityY)) {
            this.dispatchNestedFling(0.0F, (float) velocityY, canFling);
            this.fling(velocityY);
        }

    }

    private void endDrag() {
        this.mIsBeingDragged = false;
        this.recycleVelocityTracker();
        this.stopNestedScroll(0);
        if (this.mEdgeGlowTop != null) {
            this.mEdgeGlowTop.onRelease();
            this.mEdgeGlowBottom.onRelease();
        }

    }

    public void scrollTo(int x, int y) {
        if (this.getChildCount() > 0) {
            View child = this.getChildAt(0);
            x = clamp(x, this.getWidth() - this.getPaddingRight() - this.getPaddingLeft(), child.getWidth());
            y = clamp(y, this.getHeight() - this.getPaddingBottom() - this.getPaddingTop(), child.getHeight());
            if (x != this.getScrollX() || y != this.getScrollY()) {
                super.scrollTo(x, y);
            }
        }

    }

    private void ensureGlows() {
        if (this.getOverScrollMode() != 2) {
            if (this.mEdgeGlowTop == null) {
                Context context = this.getContext();
                this.mEdgeGlowTop = new EdgeEffect(context);
                this.mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            this.mEdgeGlowTop = null;
            this.mEdgeGlowBottom = null;
        }

    }

    private static int clamp(int n, int my, int child) {
        return my < child && n >= 0 ? (my + n > child ? child - my : n) : 0;
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof NestedLinearLayout.SavedState)) {
            super.onRestoreInstanceState(state);
        } else {
            NestedLinearLayout.SavedState ss = (NestedLinearLayout.SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            this.mSavedState = ss;
            this.requestLayout();
        }
    }

    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        NestedLinearLayout.SavedState ss = new NestedLinearLayout.SavedState(superState);
        ss.scrollPosition = this.getScrollY();
        return ss;
    }

    static class AccessibilityDelegate extends AccessibilityDelegateCompat {
        AccessibilityDelegate() {
        }

        public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
            if (super.performAccessibilityAction(host, action, arguments)) {
                return true;
            } else {
                NestedLinearLayout nsvHost = (NestedLinearLayout) host;
                if (!nsvHost.isEnabled()) {
                    return false;
                } else {
                    int viewportHeight;
                    int targetScrollY;
                    switch (action) {
                        case 4096:
                            viewportHeight = nsvHost.getHeight() - nsvHost.getPaddingBottom() - nsvHost.getPaddingTop();
                            targetScrollY = Math.min(nsvHost.getScrollY() + viewportHeight, nsvHost.getScrollRange());
                            if (targetScrollY != nsvHost.getScrollY()) {
                                nsvHost.smoothScrollTo(0, targetScrollY);
                                return true;
                            }

                            return false;
                        case 8192:
                            viewportHeight = nsvHost.getHeight() - nsvHost.getPaddingBottom() - nsvHost.getPaddingTop();
                            targetScrollY = Math.max(nsvHost.getScrollY() - viewportHeight, 0);
                            if (targetScrollY != nsvHost.getScrollY()) {
                                nsvHost.smoothScrollTo(0, targetScrollY);
                                return true;
                            }

                            return false;
                        default:
                            return false;
                    }
                }
            }
        }

        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            NestedLinearLayout nsvHost = (NestedLinearLayout) host;
            info.setClassName(NestedLinearLayout.class.getName());
            if (nsvHost.isEnabled()) {
                int scrollRange = nsvHost.getScrollRange();
                if (scrollRange > 0) {
                    info.setScrollable(true);
                    if (nsvHost.getScrollY() > 0) {
                        info.addAction(8192);
                    }

                    if (nsvHost.getScrollY() < scrollRange) {
                        info.addAction(4096);
                    }
                }
            }

        }

        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            NestedLinearLayout nsvHost = (NestedLinearLayout) host;
            event.setClassName(NestedLinearLayout.class.getName());
            boolean scrollable = nsvHost.getScrollRange() > 0;
            event.setScrollable(scrollable);
            event.setScrollX(nsvHost.getScrollX());
            event.setScrollY(nsvHost.getScrollY());
            AccessibilityRecordCompat.setMaxScrollX(event, nsvHost.getScrollX());
            AccessibilityRecordCompat.setMaxScrollY(event, nsvHost.getScrollRange());
        }
    }

    static class SavedState extends BaseSavedState {
        public int scrollPosition;
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public NestedLinearLayout.SavedState createFromParcel(Parcel in) {
                return new NestedLinearLayout.SavedState(in);
            }

            public NestedLinearLayout.SavedState[] newArray(int size) {
                return new NestedLinearLayout.SavedState[size];
            }
        };

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel source) {
            super(source);
            this.scrollPosition = source.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.scrollPosition);
        }

        public String toString() {
            return "NestedLinearLayout.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " scrollPosition=" + this.scrollPosition + "}";
        }
    }

    public interface OnScrollChangeListener {
        void onScrollChange(NestedLinearLayout nestedLinearLayout, int left, int top, int oldLeft, int oldTop);
    }
}
