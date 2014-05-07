package se.lundakarnevalen.extern.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.lundakarnevalen.extern.android.R;
import se.lundakarnevalen.extern.data.DataContainer;
import se.lundakarnevalen.extern.data.DataElement;
import se.lundakarnevalen.extern.data.DataType;
import se.lundakarnevalen.extern.map.Marker;
import se.lundakarnevalen.extern.util.BitmapUtil;
import se.lundakarnevalen.extern.util.Logf;

import static android.graphics.Matrix.*;
import static se.lundakarnevalen.extern.util.ViewUtil.*;

/**
 * Created by Filip on 2014-04-27.
 */
public class LKMapView extends SVGView {
    private static final float startLonMap = 13.1910161648f;
    private static final float startLatMap = 55.707371322f;

    private static final float endLonMap = 13.1979612196f;
    private static final float endLatMap = 55.7034716513f;

    private static final float diffLon = endLonMap - startLonMap;
    private static final float diffLat = endLatMap - startLatMap;

    private static final float CLOSE_THRESHOLD = 46.0f; //last 40
    public static final float BUBBLE_SIZE_MULTIPLIER = 3.0f;

    private static SparseArray<Bitmap> bitmaps = new SparseArray<Bitmap>();
    private RectF mCurrentViewPort = new RectF();

    public static void clean() {
        for(int i = 0; i < bitmaps.size(); i++) {
            bitmaps.valueAt(i).recycle();
        }
        bitmaps.clear();
    }

    public interface OnMarkerSelectedListener {
        /** null of unselect */
        public void onMarkerSelected(Marker m);
    }

    private static final String LOG_TAG = LKMapView.class.getSimpleName();

    private Set<DataType> activeTypes = new HashSet<DataType>();
    private List<Marker> markers = new ArrayList<Marker>();

    private Paint mShadowInk;
    private Paint mLightBlueInk;
    private Paint mBlueInk;

    private Picture mGpsMarker;
    private Picture mEntance;
    private Picture mBubble;

    private float mGpsMarkerSize;
    private float mGpsShadowXRadius;
    private float mGpsShadowYRadius;

    private float mBubbleSize;
    private float mBubbleShadowXRadius;
    private float mBubbleShadowYRadius;

    private PointF mGpsMarkerPos = new PointF(0,0);

    private RectF dst = new RectF();



    private float mPreDrawScale;

    private Marker mFocusedMarker;
    private OnMarkerSelectedListener mListener;

    public LKMapView(Context context) {
        super(context);
        initMap(context);
    }

    public LKMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMap(context);
    }

    public LKMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initMap(context);
    }

    private void initMap(Context context) {
        if(isInEditMode()) return;
        mShadowInk = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadowInk.setColor(Color.argb(128, 128, 128, 128));

        mBlueInk = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBlueInk.setColor(Color.rgb(74, 139, 244));

        mLightBlueInk = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLightBlueInk.setColor(Color.rgb(97, 157, 229));
        mLightBlueInk.setShadowLayer(1.5f, 0f, 0f, Color.BLACK);

        try {
            mGpsMarker = SVG.getFromResource(context, R.raw.gps_marker).renderToPicture();
            mBubble = SVG.getFromResource(context, R.raw.bubble2).renderToPicture();
            mEntance = SVG.getFromResource(context, R.raw.entre).renderToPicture();
        } catch (SVGParseException e) {
            e.printStackTrace();
        }

        mGpsMarkerSize = dpToPx(context, 80);
        mGpsShadowXRadius = dpToPx(context, 10);
        mGpsShadowYRadius = dpToPx(context, 6);

        mBubbleSize = 14.0f;//dpToPx(context, 8);
        mBubbleShadowXRadius = mBubbleSize/4.0f; //dpToPx(context, 2);
        mBubbleShadowYRadius = mBubbleSize/8.0f; //dpToPx(context, 1);

        markers.clear();
        for (DataElement elm : DataContainer.getAllData()) {
            markers.add(new Marker(elm));
        }

        initBitmapCache(context);

        if(activeTypes.size() == 0) {
            for (DataType type : DataType.values()) {
                activeTypes.add(type);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateViewLimitBounds();
    }

    private void initBitmapCache(Context context) {
        for (Marker m : markers) {
            if(bitmaps.get(m.picture) == null) {
                bitmaps.put(m.picture, BitmapUtil.decodeSampledBitmapFromResource(context.getResources(), m.picture, 224, 224));
            }
        }
    }

    public void setListener(OnMarkerSelectedListener listener) {
        this.mListener = listener;
    }

    public boolean triggerClick(float xInSvg, float yInSvg) {
        return onClick(xInSvg, yInSvg);
    }

    @Override
    protected boolean onClick(float xInSvg, float yInSvg) {

        final float offsetY = -10.0f / mPreDrawScale;
        float min = Float.MAX_VALUE;
        Marker closest = null;
        for (Marker m : markers) {
            if(activeTypes.contains(m.element.type)) {
                if (m.x != -1) {
                    final float distance = m.distance(xInSvg, yInSvg - offsetY) / mPreDrawScale;
                    if (distance < min) {
                        min = distance;
                        closest = m;
                    }
                } else {
                    m.isFocusedInMap = false;
                }
            }
        }

        boolean found = (closest != null && min < CLOSE_THRESHOLD);
        Logf.d(LOG_TAG, "click(%f, %f, %f) => Dist: %f, Closest: %s", xInSvg, yInSvg, mPreDrawScale, min, (closest != null) ? closest.element.title : closest);

        if(mFocusedMarker != null) {
            mFocusedMarker.isFocusedInMap = false;
        }

        if (found) {
            closest.isFocusedInMap = true;
            mFocusedMarker = closest;
            panTo(xInSvg, yInSvg);
            if (mListener != null) {
                mListener.onMarkerSelected(closest);
            }
        } else {
            mFocusedMarker = null;
            mListener.onMarkerSelected(null);
        }
        return false;
    }

    @Override
    protected void onDrawObjects(Canvas canvas) {
        super.onDrawObjects(canvas); // Must be called to draw map
        mPreDrawScale = mMatrixValues[MSCALE_X];

        getCurrentViewPort(mCurrentViewPort);
        mCurrentViewPort.inset(-10.0f, -10.0f);

        for (Marker m : markers) {
            if(activeTypes.contains(m.element.type)) {
                if (m.x == -1) {
                    getPointFromCoordinates(m);
                }

                if(m.isFocusedInMap || !mCurrentViewPort.contains(m.x, m.y)) continue;

                paintMarker(canvas, m);
            }
        }

        if(mFocusedMarker != null) {
            mBubbleSize *= BUBBLE_SIZE_MULTIPLIER;
            paintMarker(canvas, mFocusedMarker);
            mBubbleSize /= BUBBLE_SIZE_MULTIPLIER;
        }

        dst.set(mGpsMarkerPos.x,
                mGpsMarkerPos.y,
                mGpsMarkerPos.x + 2.0f * mGpsShadowXRadius / mPreDrawScale,
                mGpsMarkerPos.y + 2.0f * mGpsShadowYRadius / mPreDrawScale);
        normalizeToMidpointBottom(dst);
        canvas.drawOval(dst, mShadowInk);

        dst.set(mGpsMarkerPos.x,
                mGpsMarkerPos.y,
                mGpsMarkerPos.x + mGpsMarkerSize / mPreDrawScale,
                mGpsMarkerPos.y + mGpsMarkerSize / mPreDrawScale);
        normalizeToMidpointBottom(dst);
        canvas.drawPicture(mGpsMarker, dst);
    }

    private void paintMarker(Canvas canvas, Marker m) {
        if (m.element.type == DataType.ENTRANCE){
            dst.set(m.x,
                    m.y,
                    m.x + mBubbleSize * 3.0f,
                    m.y + mBubbleSize * 3.0f);
            normalizeToMidpointBottom(dst);
            canvas.drawPicture(mEntance, dst);
        } else {
            dst.set(m.x, m.y, m.x, m.y);
            dst.inset(-mBubbleShadowXRadius, -mBubbleShadowYRadius);
            canvas.drawOval(dst, mShadowInk);

            dst.set(m.x,
                    m.y,
                    m.x + mBubbleSize,
                    m.y + mBubbleSize);
            normalizeToMidpointBottom(dst);
            canvas.drawPicture(mBubble, dst);

            dst.set(m.x,
                    m.y,
                    m.x + mBubbleSize * 0.8f,
                    m.y + mBubbleSize * 0.8f);
            normalizeToMidpointBottom(dst);
            dst.offset(0f, mBubbleSize * -0.18f);
            canvas.drawBitmap(bitmaps.get(m.picture), null, dst, null);
        }
    }

    public void getPointFromCoordinates(float lat, float lon, float[] dst) {
        float lat1 = (lat - startLatMap) / diffLat;
        float lon1 = (lon - startLonMap) / diffLon;
        dst[0] = lon1 * mPictureEndPoint[AXIS_X];
        dst[1] = lat1 * mPictureEndPoint[AXIS_Y];
    }

    private void getPointFromCoordinates(Marker m) {
        float lat = (m.element.lat - startLatMap) / diffLat;
        float lon = (m.element.lng - startLonMap) / diffLon;
        m.x = lon * mPictureEndPoint[AXIS_X];
        m.y = lat * mPictureEndPoint[AXIS_Y];
    }

    private void normalizeToMidpointBottom(RectF rect) {
        rect.offset(-0.5f * rect.width(), -rect.height());
    }

    public void setGpsMarker(float lat, float lng){
        float[] tmp = new float[2];
        getPointFromCoordinates(lat, lng, tmp);
        setGpsMarker((int) tmp[AXIS_X], (int) tmp[AXIS_Y]);
    }

    public void setGpsMarker(int x, int y) {
        Logf.d(LOG_TAG, "GPSMarker moved to (%d, %d)", x, y);
        PropertyValuesHolder xh = PropertyValuesHolder.ofFloat("x",mGpsMarkerPos.x, x);
        PropertyValuesHolder yh = PropertyValuesHolder.ofFloat("y",mGpsMarkerPos.y, y);
        ValueAnimator anim = ValueAnimator.ofPropertyValuesHolder(xh, yh);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mGpsMarkerPos.x = (Float) animation.getAnimatedValue("x");
                mGpsMarkerPos.y = (Float) animation.getAnimatedValue("y");
                postInvalidate();
            }
        });
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(1000);
        anim.start();
    }

    public void setActiveTypes(Collection<DataType> types) {
        activeTypes.clear();
        activeTypes.addAll(types);
        postInvalidate();
    }
}
