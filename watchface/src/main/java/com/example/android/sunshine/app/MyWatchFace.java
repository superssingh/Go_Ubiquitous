/*
 * Created by Santosh Kumar Singh on 18/04/2016 on 6:18 PM
 * Copyright (c) 2016 All rights reserved.
 *
 *  Last modified 11/24/16 12:20 AM
 *
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle.Builder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.support.wearable.watchface.WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE;
import static android.support.wearable.watchface.WatchFaceStyle.PEEK_MODE_VARIABLE;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {

    private static final String TAG = MyWatchFace.class.getSimpleName();
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(500);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        final Handler mUpdateTime = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint, mDatePaint, mWeatherPaint, mTempraturePaint;
        boolean mAmbient;
        Calendar mCalendar;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffset;
        float mYOffset;
        String maxTemp = "24", minTemp = "15";
        int weatherId = 200;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        private GoogleApiClient googleApiClient;
        private Bitmap mBackgroundBitmap;
        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        private int mWatchHandColor;
        private int mWatchHandHighlightColor;
        private int mWatchHandShadowColor;
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new Builder(MyWatchFace.this)
                    .setCardPeekMode(PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTempraturePaint = new Paint();
            mTempraturePaint = createTextPaint(resources.getColor(R.color.digital_text));

            mDatePaint = new Paint();
            mDatePaint = createTextPaint(resources.getColor(R.color.digital_text));

            mWeatherPaint= new Paint();
            mWeatherPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mCalendar = Calendar.getInstance();

            //For Weather Update
            googleApiClient = new GoogleApiClient.Builder(MyWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        @Override
        public void onDestroy() {
            mUpdateTime.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Connect for receiving message from mobile
                googleApiClient.connect();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            googleApiClient.disconnect();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
            mDatePaint.setTextSize(textSize/5);
            mWeatherPaint.setTextSize(textSize/2);
            mTempraturePaint.setTextSize(textSize/4);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mDatePaint.setAntiAlias(!inAmbientMode);
                    mWeatherPaint.setAntiAlias(!inAmbientMode);
                    mTempraturePaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            String text =  String.format("%02d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE));
            canvas.drawText(text, bounds.width()/4, bounds.height()/2, mTextPaint);

            float lowerYOffset = mYOffset + mTextPaint.getTextSize() + 20;

            canvas.drawText(mCalendar.get(Calendar.DAY_OF_MONTH) + " " +
                            month(mCalendar.get(Calendar.MONTH)) + " " +
                            new String(mCalendar.get(Calendar.YEAR)+"").substring(2,4) + ", " +
                            day(mCalendar.get(Calendar.DAY_OF_WEEK))
                    , (bounds.width()/3), lowerYOffset-35, mDatePaint);

            if(!isInAmbientMode()) {
                int icon = getWeatherIcon(weatherId);
                Bitmap weatherIcon = BitmapFactory.decodeResource(getResources(), icon);
                canvas.drawBitmap(weatherIcon, (bounds.width()/5), bounds.height()/10-10, mWeatherPaint);
            }

            int absoluteTemp = (Integer.parseInt(maxTemp) +Integer.parseInt(minTemp))/2;
            int rowPosition=bounds.centerX()+5;

            canvas.drawText(getWeatherName(weatherId), bounds.centerX(), bounds.height()/4, mTempraturePaint);
            canvas.drawText(absoluteTemp+"\u00b0"+"C", bounds.width()/3-30, lowerYOffset+30, mWeatherPaint);
            canvas.drawLine(rowPosition, lowerYOffset-10, rowPosition,lowerYOffset+50, mTextPaint);
            canvas.drawLine(rowPosition, lowerYOffset+20, rowPosition+(rowPosition/2),lowerYOffset+20, mTextPaint);
            canvas.drawText(maxTemp+"\u00b0"+"C max", rowPosition+10, lowerYOffset+10, mTempraturePaint);
            canvas.drawText(minTemp+"\u00b0"+"C min", rowPosition+10, lowerYOffset+45, mTempraturePaint);

        }

        /**
         * Starts the {@link #mUpdateTime} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTime.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTime.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTime} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTime.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(googleApiClient, Engine.this);
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d("Data Change", "Changed");
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    getTempratureInfo(item);
                }
            }

            dataEvents.release();
            invalidate();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d("Connection","Suspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d("Connection","Fail"+connectionResult.getErrorMessage());
        }

        private void getTempratureInfo(DataItem item) {
            if ("/wear_face".equals(item.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                if (dataMap.containsKey("HIGH_TEMP"))
                    maxTemp = dataMap.getString("HIGH_TEMP");
                if (dataMap.containsKey("LOW_TEMP"))
                    minTemp = dataMap.getString("LOW_TEMP");
                if (dataMap.containsKey("WEATHER_ID"))
                    weatherId = dataMap.getInt("WEATHER_ID");
            }
        }
        private void updateWatchHandStyle(){
                mTextPaint.setColor(mWatchHandColor);
                mDatePaint.setColor(mWatchHandColor);
                mWeatherPaint.setColor(mWatchHandHighlightColor);
                mTempraturePaint.setColor(mWatchHandColor);

                mTextPaint.setAntiAlias(true);
                mDatePaint.setAntiAlias(true);
                mWeatherPaint.setAntiAlias(true);
                mTempraturePaint.setAntiAlias(true);
        }
    }

    public static int getWeatherIcon(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return R.drawable.ic_status;
    }

    public static String getWeatherName(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return "Thunderstorm";
        } else if (weatherId >= 300 && weatherId <= 321) {
            return "Drizzle";
        } else if (weatherId >= 500 && weatherId <= 504) {
            return "Rain";
        } else if (weatherId == 511) {
            return "Snow";
        } else if (weatherId >= 520 && weatherId <= 531) {
            return "Rain";
        } else if (weatherId >= 600 && weatherId <= 622) {
            return "Snow";
        } else if (weatherId >= 701 && weatherId <= 761) {
            return "Foggy";
        } else if (weatherId == 761 || weatherId == 781) {
            return "Storm";
        } else if (weatherId == 800) {
            return "Clear Sky";
        } else if (weatherId == 801) {
            return "Light Clouds";
        } else if (weatherId >= 802 && weatherId <= 804) {
            return "Cloudy";
        }
        return null;
    }

    public static String month(int month) {
        if(month>=0 && month<=11) {
            if (month == 0)
                return "Jan";
            else if (month == 1)
                return "Feb";
            else if (month == 2)
                return "Mar";
            else if (month == 3)
                return "Apr";
            else if (month == 4)
                return "May";
            else if (month == 5)
                return "Jun";
            else if (month == 6)
                return "Jul";
            else if (month == 7)
                return "Aug";
            else if (month == 8)
                return "Sep";
            else if (month == 9)
                return "Oct";
            else if (month == 10)
                return "Nov";
            else if (month == 11)
                return "Dec";
        }
        return null;
    }

    public static String day(int day) {
        if(day>=1 && day<=7) {
            if (day == 1)
                return "Sunday";
            else if (day == 2)
                return "Monday";
            else if (day == 3)
                return "Tuesday";
            else if (day == 4)
                return "Wednesday";
            else if (day == 5)
                return "Thursday";
            else if (day == 6)
                return "Friday";
            else if (day == 7)
                return "Saturday";
        }
        return null;
    }

}