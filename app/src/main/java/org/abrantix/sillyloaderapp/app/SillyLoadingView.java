package org.abrantix.sillyloaderapp.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Created by fabrantes on 09/04/14.
 */
public class SillyLoadingView extends View {
    public SillyLoadingView(Context context) {
        super(context);
        init(context, null, 0);
    }
    public SillyLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }
    public SillyLoadingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public void init (Context ctx, AttributeSet attrs, int defStyle) {
        TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.SillyLoadingView, defStyle,
                0);
        if (ta != null) {
            mStrokeColor = ta.getColor(R.styleable.SillyLoadingView_color, Color.WHITE);
            mBounceAngle = ta.getFloat(R.styleable.SillyLoadingView_angleBounce, 30);
            mAngleDelta = ta.getFloat(R.styleable.SillyLoadingView_angleDelta, 340);
            mStepAngleDelta = ta.getFloat(R.styleable.SillyLoadingView_stepAngleDelta, 180);
            mStrokeWidth = ta.getDimensionPixelSize(R.styleable.SillyLoadingView_strokeWidth, 5);
            mDuration = ta.getInteger(R.styleable.SillyLoadingView_duration, 999);
            mDesyncFactor = ta.getFloat(R.styleable.SillyLoadingView_desyncFactor, .5f);
            ta.recycle();
        }
    }

    public interface Listener {
        public void onFinished();
    }

    private static enum State {
        INIT,
        PAUSED,
        FIRST_LOAD,
        LOADING,
        FINISHING,
        CLOSING_OUT,
        CLOSING_OUT2,
        FINISHED
    }

    private State mState = State.INIT;

    Path mPath = new Path();
    Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    float mAngleDelta = 340;
    float mStartAngle = 0;
    float mStopAngle = 0;
    float mBounceAngle = 30;

    float mCenterX = 0;
    float mCenterY = 0;
    RectF mContent = new RectF();

    float mStrokeWidth = 10.0f;
    int mStrokeColor = Color.argb(0xff, 0xef, 0xef, 0xef);

    float mDuration = 999;
    float mCounter = 0;

    float mIStartAngle = mStartAngle;
    float mTStartAngle = mStartAngle;
    float mIStopAngle = mStopAngle;
    float mTStopAngle = mStopAngle;

    float mStepAngleDelta = 90;
    float mDesyncFactor = 0.5f;

    Listener mListener;

    public void setListener(Listener l) {
        mListener = l;
    }

    public void start() {
        if (mState == State.INIT || mState == State.FINISHED || mState == State.FINISHING ||
                mState == State.CLOSING_OUT || mState == State.CLOSING_OUT2) {
            mStartAngle = 0;
            mStopAngle = 0;
            mIStartAngle = mStartAngle;
            mIStopAngle = mStopAngle;
            mTStartAngle = 0;
            mTStopAngle = mTStartAngle + mAngleDelta;
            mState = State.FIRST_LOAD;
            mCounter = 0;
        } else if (mState == State.LOADING) {
            mState = State.LOADING;
        } else if (mState == State.PAUSED) {
            mState = State.LOADING;
        } else {
            mState = State.LOADING;
        }
        oLastTimestamp = 0;
        invalidate();
    }

    public void pause() {
        mState = State.PAUSED;
        invalidate();
    }

    boolean mPendingFinish = false;

    public void finish() {
        if (mState == State.FINISHING || mState == State.FINISHED || mState == State.CLOSING_OUT
                || mState == mState.CLOSING_OUT2)
            return;

        mPendingFinish = true;
        //mState = State.FINISHING;

        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float strokeHalfWidth = mStrokeWidth * 0.5f;
        mContent.left = getPaddingLeft() + strokeHalfWidth;
        mContent.top = getPaddingTop() + strokeHalfWidth;
        mContent.right = w - getPaddingRight() - strokeHalfWidth;
        mContent.bottom = h - getPaddingBottom() - strokeHalfWidth;

        mCenterX = mContent.left + (mContent.width() * .5f);
        mCenterY = mContent.top + (mContent.height() * .5f);

        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(mStrokeColor);
    }

    private float getDuration() {
        switch (mState) {
            case CLOSING_OUT:
                return 0.85f * mDuration;
            case CLOSING_OUT2:
                return 1.1f * mDuration;
            case FIRST_LOAD:
                return 1.33f * mDuration;
            default:
                return mDuration;
        }
    }

    private float getDesyncFactor() {
        return mDesyncFactor;
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        if (mState == State.FINISHED || mState == State.INIT)
            return;

        step();
        adjustPath(mPath, mStartAngle, mStopAngle);
        c.drawPath(mPath, mPaint);

        if (shouldRedraw())
            invalidate();
    }

    public boolean shouldRedraw() {
        return mState != State.FINISHED && mState != State.PAUSED && mState != State.INIT;
    }

    float oDt;
    long oLastTimestamp = 0;
    float oStartAngleFraction;
    float oStopAngleFraction;
    float oExtraStartAngleFraction;
    float oExtraStopAngleFraction;

    Interpolator mInterpolatorStartAngle = new DecelerateInterpolator();
    Interpolator mInterpolatorStopAngle = new AccelerateInterpolator();

    boolean doBounce = true;

    private void step() {
        if (oLastTimestamp == 0) {
            oDt = 1;
        } else {
            oDt = System.currentTimeMillis() - oLastTimestamp;
        }
        oLastTimestamp = System.currentTimeMillis();
        float duration = getDuration();
        float desyncFactor = getDesyncFactor();

        mCounter += oDt;
        mCounter = Math.min(duration, mCounter);

        oStartAngleFraction = mCounter / ((1 - desyncFactor) * duration);
        oStopAngleFraction = (mCounter - desyncFactor * duration) / ((1 - desyncFactor) *
                duration);
        oExtraStartAngleFraction = Math.max(0, oStartAngleFraction - 1);
        oExtraStopAngleFraction = Math.min(0, - desyncFactor / (1 - desyncFactor) -
                oStopAngleFraction);

        oStartAngleFraction = Math.max(0f, Math.min(oStartAngleFraction, 1));
        oStopAngleFraction = Math.max(0f, Math.min(oStopAngleFraction, 1));

        mStartAngle = mIStartAngle + (mTStartAngle - mIStartAngle) * mInterpolatorStartAngle
                .getInterpolation(oStartAngleFraction);
        mStopAngle = mIStopAngle + (mTStopAngle - mIStopAngle) * mInterpolatorStopAngle
                .getInterpolation(oStopAngleFraction);

        if (doBounce && mState != State.CLOSING_OUT2 && mState != State.FIRST_LOAD) {
            mStopAngle -= (mBounceAngle) * mInterpolatorStartAngle.getInterpolation
                    (-oExtraStopAngleFraction);
        }

        if (mCounter >= duration) {
            mCounter = 0;

            if (mState == State.FINISHING) {
                mState = State.CLOSING_OUT;
                mIStartAngle = mStartAngle;
                mIStopAngle = mStopAngle;
                mTStartAngle += mStepAngleDelta;
                mTStopAngle = mTStartAngle + 360.0f;
                return;
            } else if (mState == State.CLOSING_OUT) {
                mState = State.CLOSING_OUT2;
                mIStartAngle = mStartAngle;
                mIStopAngle = mStopAngle;
                mTStartAngle = mTStartAngle;
                mTStopAngle = mTStartAngle;
                return;
            } else if (mState == State.CLOSING_OUT2) {
                mState = State.FINISHED;
                if (mListener != null)
                    mListener.onFinished();
                return;
            } else {
                if (mPendingFinish) {
                    mState = State.FINISHING;
                    mPendingFinish = false;
                } else if (mState == State.FIRST_LOAD) {
                    mState = State.LOADING;
                }
                mIStartAngle = mStartAngle;
                mIStopAngle = mStopAngle;
                mTStopAngle += mStepAngleDelta;
                mTStartAngle += mStepAngleDelta;
            }
        }

        return;
    }

    private void adjustPath(Path path, float startAngle, float stopAngle) {
        path.reset();

        path.addArc(mContent, startAngle, (stopAngle - startAngle));
    }
}
