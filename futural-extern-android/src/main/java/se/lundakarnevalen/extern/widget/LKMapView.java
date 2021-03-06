package se.lundakarnevalen.extern.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.lundakarnevalen.extern.android.R;
import se.lundakarnevalen.extern.data.DataContainer;
import se.lundakarnevalen.extern.data.DataElement;
import se.lundakarnevalen.extern.data.DataType;
import se.lundakarnevalen.extern.map.LocationTracker;
import se.lundakarnevalen.extern.map.Marker;
import se.lundakarnevalen.extern.util.BitmapUtil;
import se.lundakarnevalen.extern.util.Logf;

import static android.graphics.Matrix.MSCALE_X;

public class LKMapView extends SVGView {
    private static final String LOG_TAG = LKMapView.class.getSimpleName();

    private static final float startLonMap = 13.1910161648f;
    private static final float startLatMap = 55.707371322f;

    private static final float endLonMap = 13.1979612196f;
    private static final float endLatMap = 55.7034716513f;

    private static final float diffLon = endLonMap - startLonMap;
    private static final float diffLat = endLatMap - startLatMap;

    private static final float CLOSE_THRESHOLD = 14.0f; //last 40
    private static final float BUBBLE_SIZE_MULTIPLIER = 3.0f;

    private static SparseArray<Bitmap> bitmaps = new SparseArray<>();
    private RectF mCurrentViewPort = new RectF();
    private boolean mFiltersEnabled = true;

    private float mDevMarkusX = -1.0f;
    private float mDevMarkusY = -1.0f;
    private float mDevFilipX = -1.0f;
    private float mDevFilipY = -1.0f;
    private float mDevFredrikX = -1.0f;
    private float mDevFredrikY = -1.0f;
    private float mDevSize;

    private Paint mMarkusInk;
    private Paint mFilipInk;
    private Paint mFredrikInk;

    public static void clean() {
        for(int i = 0; i < bitmaps.size(); i++) {
            bitmaps.valueAt(i).recycle();
        }
        bitmaps.clear();
    }

    public boolean isWithinLatLngRange(float lat, float lng) {
        return  (startLatMap > lat && lat > endLatMap) &&
                (startLonMap < lng && lng < endLonMap);
    }

    public void setDevLatLng(LocationTracker.LocationJSONResult.LatLng markus,
                             LocationTracker.LocationJSONResult.LatLng filip,
                             LocationTracker.LocationJSONResult.LatLng fredrik) {
        float[] xy = new float[2];
        getPointFromCoordinates(markus.lat, markus.lng, xy);
        mDevMarkusX = xy[0];
        mDevMarkusY = xy[1];
        getPointFromCoordinates(filip.lat, filip.lng, xy);
        mDevFilipX = xy[0];
        mDevFilipY = xy[1];
        getPointFromCoordinates(fredrik.lat, fredrik.lng, xy);
        mDevFredrikX = xy[0];
        mDevFredrikY = xy[1];
    }

    public interface OnMarkerSelectedListener {
        /** null of unselect */
        void onMarkerSelected(Marker m);
    }


    private Set<DataType> activeTypes = new HashSet<>();
    private List<Marker> markers = new ArrayList<>();

    private Paint mShadowInk;
    private Paint mLightBlueInk;
    private Paint mBlueInk;

    private Picture mGpsMarker;
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

        mMarkusInk = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMarkusInk.setColor(Color.rgb(212,0,255));
        //mMarkusInk.setShadowLayer(1.5f, 0f, 0f, Color.BLACK);

        mFilipInk = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFilipInk.setColor(Color.rgb(255, 85, 170));
        //mFilipInk.setShadowLayer(1.5f, 0f, 0f, Color.BLACK);

        mFredrikInk = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFredrikInk.setColor(Color.rgb(255, 165, 0));
        //mFredrikInk.setShadowLayer(1.5f, 0f, 0f, Color.BLACK);

        mLightBlueInk = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLightBlueInk.setColor(Color.rgb(97, 157, 229));
        mLightBlueInk.setShadowLayer(1.5f, 0f, 0f, Color.BLACK);

        try {
            mGpsMarker = SVG.getFromResource(context, R.raw.gps_marker).renderToPicture();
            mBubble = SVG.getFromResource(context, R.raw.bubble).renderToPicture();
        } catch (SVGParseException e) {
            e.printStackTrace();
        }

        /** Will be recalculated in updateViewLimitBounds() */
        mGpsMarkerSize = 34.0f; //dpToPx(context, 80);
        mGpsShadowXRadius = mGpsMarkerSize / 6.0f;//dpToPx(context, 10);
        mGpsShadowYRadius = mGpsMarkerSize / 12.0f; //dpToPx(context, 6);

        /** Will be recalculated in updateViewLimitBounds() */
        mBubbleSize = 14.0f; //dpToPx(context, 8);
        mBubbleShadowXRadius = mBubbleSize / 4.0f; //dpToPx(context, 2);
        mBubbleShadowYRadius = mBubbleSize / 8.0f; //dpToPx(context, 1);

        mDevSize = 7.5f;

        markers.clear();
        for (DataElement elm : DataContainer.getAllData()) {
            markers.add(new Marker(elm));
        }

        Collections.sort(markers);

        initBitmapCache(context);
        initZoomLimit();
    }

    /**
     * We want to have difirent
     */
    private void initZoomLimit() {
        final Resources r = getResources();
        if(r != null && r.getDisplayMetrics() != null) {
            final int dpi = r.getDisplayMetrics().densityDpi;
            switch (dpi){
                case DisplayMetrics.DENSITY_XXXHIGH: setMaxZoom(14.0f); break;
                case DisplayMetrics.DENSITY_XXHIGH: setMaxZoom(12.0f); break;
                case DisplayMetrics.DENSITY_XHIGH: setMaxZoom(10.0f); break;
                case DisplayMetrics.DENSITY_HIGH: setMaxZoom(6.0f); break;
                case DisplayMetrics.DENSITY_MEDIUM: setMaxZoom(5.0f); break;
                case DisplayMetrics.DENSITY_LOW: setMaxZoom(5.0f); break;

                default: setMaxZoom(5.5f); break;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateViewLimitBounds();
    }

    private void initBitmapCache(Context context) {
        int hw = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? 224 : 56;
        for (Marker m : markers) {
            if(bitmaps.get(m.picture) == null) {
                bitmaps.put(m.picture, BitmapUtil.decodeSampledBitmapFromResource(context.getResources(), m.picture, hw, hw));
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

        if(xInSvg + yInSvg < 100 && mListener != null) {
            for (Marker m : markers) {
                if(m.element.type == DataType.DEVELOPER){
                    panTo(m.x, m.y);
                    mFocusedMarker = m;
                    m.isFocusedInMap = true;
                    mListener.onMarkerSelected(m);
                    return true;
                }
            }
        }

        if(addExtra > 0) {
            yInSvg += addExtra/mPreDrawScale;
        }

        float min = Float.MAX_VALUE;
        Marker closest = null;
        for (Marker m : markers) {
            if(activeTypes.contains(m.element.type)) {
                if (m.x != -1) {
                    final float distance = m.distance(xInSvg, yInSvg, mBubbleSize) / mPreDrawScale;
                    if (distance < min) {
                        min = distance;
                        closest = m;
                    }
                } else {
                    m.isFocusedInMap = false;
                }
            }
        }

        boolean found = (closest != null && min < CLOSE_THRESHOLD * mPreDrawScale);
        Logf.d(LOG_TAG, "click(x:%f, y:%f, scale: %f) => Dist: %f, Closest: %s", xInSvg, yInSvg, mPreDrawScale, min, closest);

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

    public void goToGpsMarker(int svgX, int svgY){


        if(mFocusedMarker != null) {
            mFocusedMarker.isFocusedInMap = false;
        }
        mFocusedMarker = null;
        mListener.onMarkerSelected(null);

        panTo(svgX,svgY);
    }

    @Override
    protected void onDrawObjects(Canvas canvas) {
        super.onDrawObjects(canvas); // Must be called to draw map
        mPreDrawScale = mMatrixValues[MSCALE_X];

        getCurrentViewPort(mCurrentViewPort);
        mCurrentViewPort.inset(-10.0f, -10.0f);

        for (Marker m:markers) {
            if(activeTypes.contains(m.element.type)) {
                if (m.x == -1.0f) {
                    getPointFromCoordinates(m);
                }

                if(m.isFocusedInMap || !mCurrentViewPort.contains(m.x, m.y)) continue;

                paintMarker(canvas, m);
            }
        }

        if(mFocusedMarker != null) {
            paintMarkerFocused(canvas, mFocusedMarker);
        }

        dst.set(mGpsMarkerPos.x,
                mGpsMarkerPos.y,
                mGpsMarkerPos.x + 2.0f * mGpsShadowXRadius,
                mGpsMarkerPos.y + 2.0f * mGpsShadowYRadius);
        normalizeToMidpointBottom(dst);
        canvas.drawOval(dst, mShadowInk);

        dst.set(mGpsMarkerPos.x,
                mGpsMarkerPos.y,
                mGpsMarkerPos.x + mGpsMarkerSize,
                mGpsMarkerPos.y + mGpsMarkerSize);
        normalizeToMidpointBottom(dst);
        canvas.drawPicture(mGpsMarker, dst);

        canvas.drawCircle(mDevMarkusX, mDevMarkusY, mDevSize / mPreDrawScale, mMarkusInk);
        canvas.drawCircle(mDevFilipX, mDevFilipY, mDevSize / mPreDrawScale, mFilipInk);
        canvas.drawCircle(mDevFredrikX, mDevFredrikY, mDevSize / mPreDrawScale, mFredrikInk);
    }

    private void paintMarker(Canvas canvas, Marker m) {
        if (m.element.type == DataType.ENTRANCE){
            dst.set(m.x,
                    m.y,
                    m.x + mBubbleSize * 2.0f,
                    m.y + mBubbleSize * 1.18f);
            dst.offset(-0.5f * dst.width(), -0.5f * dst.height()); // center in XY
            canvas.drawBitmap(bitmaps.get(m.picture), null, dst, null);
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

    private void paintMarkerFocused(Canvas canvas, Marker m) {
        if (m.element.type == DataType.ENTRANCE){
            dst.set(m.x,
                    m.y,
                    m.x + mBubbleSize * 3.0f,
                    m.y + mBubbleSize * 1.76f);
            dst.offset(-0.5f * dst.width(), -0.5f * dst.height()); // center in XY
            canvas.drawBitmap(bitmaps.get(m.picture), null, dst, null);
        } else {
            dst.set(m.x, m.y, m.x, m.y);
            dst.inset(-mBubbleShadowXRadius, -mBubbleShadowYRadius);
            canvas.drawOval(dst, mShadowInk);
            mBubbleSize *= BUBBLE_SIZE_MULTIPLIER;
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
            mBubbleSize /= BUBBLE_SIZE_MULTIPLIER;
        }
    }

    public void getPointFromCoordinates(float lat, float lon, float[] dst) {
        float lat1 = (lat - startLatMap) / diffLat;
        float lon1 = (lon - startLonMap) / diffLon;
        if(mPictureEndPoint[AXIS_X]==-1) {
            dst[0] = lon1 * 512;
            dst[1] = lat1 * 512;
        } else {
            dst[0] = lon1 * mPictureEndPoint[AXIS_X];
            dst[1] = lat1 * mPictureEndPoint[AXIS_Y];
        }
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

    public void setGpsMarker(float lat, float lng, boolean panToMarker) {
        float[] tmp = new float[2];
        getPointFromCoordinates(lat, lng, tmp);
        setGpsMarker((int) tmp[AXIS_X], (int) tmp[AXIS_Y], panToMarker); // (int) is important to avoid recursion
    }

    @Override
    protected void filterMatrix(Matrix matrix) {
        if (mFiltersEnabled) {
            super.filterMatrix(matrix);
        }
    }

    public void setGpsMarker(int x, int y, boolean panToMarker) {
        Logf.d(LOG_TAG, "GPSMarker moved to (%d, %d)", x, y);
        PropertyValuesHolder xh = PropertyValuesHolder.ofFloat("x", mGpsMarkerPos.x, x);
        PropertyValuesHolder yh = PropertyValuesHolder.ofFloat("y", mGpsMarkerPos.y, y);
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

        if(panToMarker) { // Will fire of async as the above animation
            panTo(x, y);
        }
    }


    private final float[] mRotationTmp = new float[2];

    public void setRotationLatLng(float lat, float lng, float degree) {
        getPointFromCoordinates(lat, lng, mRotationTmp);
        setRotation(mRotationTmp[AXIS_X], mRotationTmp[AXIS_Y], degree);
    }

    public void setRotation(float x, float y, float degree) {
        panTo(x, y, false);
        mMatrix.postRotate(degree, x, y);
        postInvalidate();
    }

    public void setActiveTypes(Collection<DataType> types) {
        activeTypes.clear();
        activeTypes.addAll(types);
        postInvalidate();
    }

    @Override
    public boolean updateViewLimitBounds() {
        final boolean hasLayoutAndBounds = super.updateViewLimitBounds();
        if(hasLayoutAndBounds) {
            mBubbleSize = (mViewEndPoint[AXIS_X] * 0.04f)/mMinZoom;//dpToPx(context, 8);
            mBubbleShadowXRadius = mBubbleSize/4.0f; //dpToPx(context, 2);
            mBubbleShadowYRadius = mBubbleSize/8.0f; //dpToPx(context, 1);
            mGpsMarkerSize = (mViewEndPoint[AXIS_X] * 0.08f)/mMinZoom;//dpToPx(context, 8);
            mGpsShadowXRadius = mBubbleSize/4.0f; //dpToPx(context, 2);
            mGpsShadowYRadius = mBubbleSize/8.0f; //dpToPx(context, 1);
        }
        return hasLayoutAndBounds;
    }
}
