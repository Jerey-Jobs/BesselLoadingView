package com.jerey.besselloadingviewlib;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by xiamin on 3/22/17.
 */

public class BesselLoadingView extends View {
    private static final String TAG = "BesselLoadingView";
    private static final boolean ISDEBUG = false;
    private static final int DEFAULT_RADIUS = 80;
    private static final int DEFAULT_DURATION = 1500;
    private static final int DEFAULT_COLOR = 0xff00dddd;
    //定圆半径
    private float mRadius;
    //动圆半径
    private float mRadiusFloat;
    //颜色
    private int mLoadingColor;
    //动画时间
    private int mDuration;
    //三个圆的X圆心
    private int[] mCirclesX;
    //三个圆的Y圆心
    private int mCirClesY;
    //滚动的X圆心
    private float mFloatX;
    //两个圆心相距多少时开始贝塞尔曲线
    private int mMinDistance;

    private Paint mPaint;
    private Path mPath;

    public BesselLoadingView(Context context) {
        this(context, null);
    }

    public BesselLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BesselLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initUI(context, attrs);
    }

    private void initUI(Context context, AttributeSet attrs) {
        mPaint = new Paint();
        //路径
        mPath = new Path();
        mCirclesX = new int[3];
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BesselLoadingView);
        mLoadingColor = ta.getColor(R.styleable.BesselLoadingView_loadingcolor, DEFAULT_COLOR);
        mRadius = ta.getDimension(R.styleable.BesselLoadingView_loadingradius, DEFAULT_RADIUS);
        mDuration = ta.getInt(R.styleable.BesselLoadingView_loadingduration, DEFAULT_DURATION);

        mPaint.setColor(mLoadingColor);
        mPaint.setAntiAlias(true); //抗锯齿

        mRadiusFloat = mRadius * 0.9f;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mWidth;
        int mHeight;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            mWidth = widthSize;
        } else {
            mWidth = getPaddingLeft() + 480 + getPaddingRight();
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            mHeight = heightSize;
        } else {
            mHeight = getPaddingTop() + 100 + getPaddingBottom();
        }

        setMeasuredDimension(mWidth, mHeight);
        log("width: " + mWidth + " h: " + mHeight);
        //计算x方向三个圆心   -.-.-.-
        int lenth = mWidth / 4;
        for (int i = 0; i < 3; i++) {
            mCirclesX[i] = lenth * (i + 1);
        }

        //计算三个圆心Y坐标
        mCirClesY = mHeight / 2;
        //三个初始圆的半径
        mRadius = mHeight / 3;
        mRadiusFloat = mRadius * 0.9f;
        log("mCirclesX: " + mCirclesX[0] + "," + mCirclesX[1] + "," + mCirclesX[2] + "  Y: " + mCirClesY);

        if (mRadius >= lenth / 4) {
            log("圆的半径大于间隙了,自动缩小");
            mRadius = lenth / 4;
            mRadiusFloat = mRadius * 0.9f;
        }

        mMinDistance = lenth;

        log("mMinDistance " + mMinDistance);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(mRadius, mWidth - mRadius);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setDuration(mDuration);
        valueAnimator.setRepeatMode(ValueAnimator.REVERSE);
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFloatX = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        valueAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //画三个圆
        for (int i = 0; i < 3; i++) {
            canvas.drawCircle(mCirclesX[i], mCirClesY, mRadius, mPaint);
        }
        //画滑动圆
        canvas.drawCircle(mFloatX, mCirClesY, mRadiusFloat, mPaint);

        drawBesselLine(canvas);
    }

    /**
     * 绘制贝塞尔曲线与定点圆变大
     *
     * @param canvas
     */
    private void drawBesselLine(Canvas canvas) {
        float minDis = mMinDistance;
        int minLocation = 0;
        for (int i = 0; i < 3; i++) {
            float dis = Math.abs((mFloatX - mCirclesX[i]));
            if (dis < minDis) {
                minDis = dis;
                minLocation = i;
            }
        }
        // log("最小距离为 " + minDis + "位置:" + minLocation);
        if (minDis < mMinDistance) {

            float middleX = (mCirclesX[minLocation] + mFloatX) / 2;
            //绘制上半部分贝塞尔曲线
            mPath.moveTo(mCirclesX[minLocation], mCirClesY + mRadius);
            mPath.quadTo(middleX, mCirClesY, mFloatX, mCirClesY + mRadiusFloat);

            mPath.lineTo(mFloatX, mCirClesY - mRadiusFloat);

            mPath.quadTo(middleX, mCirClesY, mCirclesX[minLocation], mCirClesY - mRadius);

            mPath.lineTo(mCirclesX[minLocation], mCirClesY + mRadius);
            mPath.close();

            canvas.drawPath(mPath, mPaint);
            mPath.reset();
            //浮动圆靠近固定圆变大
            float f = 1 + (mMinDistance - minDis * 2) / mMinDistance * 0.2f;
            log("dis% : " + (mMinDistance - minDis) / mMinDistance + "  f = " + f);
            canvas.drawCircle(mCirclesX[minLocation], mCirClesY, mRadius * f, mPaint);
        }
    }


    private void log(String string) {
        if (ISDEBUG) {
            Log.d(TAG, string);
        }
    }
}
