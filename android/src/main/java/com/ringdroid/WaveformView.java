/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ringdroid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.ringdroid.soundfile.SoundFile;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;

/**
 * WaveformView is an Android view that displays a visual representation
 * of an audio waveform.  It retrieves the frame gains from a CheapSoundFile
 * object and recomputes the shape contour at several zoom levels.
 *
 * This class doesn't handle selection or any of the touch interactions
 * directly, so it exposes a listener interface.  The class that embeds
 * this view should add itself as a listener and make the view scroll
 * and respond to other events appropriately.
 *
 * WaveformView doesn't actually handle selection, but it will just display
 * the selected part of the waveform in a different color.
 */
public class WaveformView extends View {
    public void setmURI(String mURI) {
        this.mURI = mURI;
        new DownloadFileFromURL().execute(this.mURI);








    }

    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, SoundFile> {


        String filePath = Environment.getExternalStorageDirectory().toString() + "/"+random()+".mp3";

        /**
         * Before starting background thread Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected SoundFile doInBackground(String... f_url) {
            int count;
            SoundFile soundFile = null;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(filePath);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
                Log.e("XSXGOT","Audio file complented + "+filePath);

            } catch (Exception e) {
                Log.e("XSXGOT Error: ", e.getMessage());
            }

            try {
                soundFile = SoundFile.create(filePath,null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SoundFile.InvalidInputException e) {
                e.printStackTrace();
            }

            return soundFile;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage

        }



        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(SoundFile soundFile) {
            // dismiss the dialog after the file was downloaded
            if(soundFile!=null) {
                setSoundFile(soundFile);
                recomputeHeights(1f);
                invalidate();
            }else{
                Log.e("XSXGOT","soundfile is null");
            }

        }

    }


    private   String random() {
        return new BigInteger(130, new SecureRandom()).toString(32);
    }

    public interface WaveformListener {
        public void waveformTouchStart(float x);
        public void waveformTouchMove(float x);
        public void waveformTouchEnd();
        public void waveformFling(float x);
        public void waveformDraw();
        public void waveformZoomIn();
        public void waveformZoomOut();
    };

    // Colors
    private Paint teswtPaint;
    private Paint mGridPaint;
    private Paint mSelectedLinePaint;
    private Paint mUnselectedLinePaint;
    private Paint mUnselectedBkgndLinePaint;
    private Paint mBorderLinePaint;
    private Paint mPlaybackLinePaint;
    private Paint mTimecodePaint;

    private String mURI;

    private SoundFile mSoundFile;
    private int[] mLenByZoomLevel;
    private double[][] mValuesByZoomLevel;
    private double[] mZoomFactorByZoomLevel;
    private int[] mHeightsAtThisZoomLevel;
    private int mZoomLevel;
    private int mNumZoomLevels;
    private int mSampleRate;
    private int mSamplesPerFrame;
    private int mOffset;
    private int mSelectionStart;
    private int mSelectionEnd;
    private int mPlaybackPos;
    private float mDensity;
    private float mInitialScaleSpan;
    private WaveformListener mListener;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private boolean mInitialized;

    public WaveformView(Context context) {
        super(context);

        // We don't want keys, the markers get these
        setFocusable(false);

        mGridPaint = new Paint();
        mGridPaint.setAntiAlias(false);
        mGridPaint.setColor(Color.GREEN);

        teswtPaint = new Paint();
        teswtPaint.setAntiAlias(false);
        teswtPaint.setColor(Color.WHITE);

        mSelectedLinePaint = new Paint();
        mSelectedLinePaint.setAntiAlias(false);
        mSelectedLinePaint.setColor(Color.WHITE);
        mUnselectedLinePaint = new Paint();
        mUnselectedLinePaint.setAntiAlias(false);
        mUnselectedLinePaint.setColor(Color.WHITE);
        mUnselectedBkgndLinePaint = new Paint();
        mUnselectedBkgndLinePaint.setAntiAlias(false);
        mUnselectedBkgndLinePaint.setColor(Color.TRANSPARENT);
        mBorderLinePaint = new Paint();
        mBorderLinePaint.setAntiAlias(true);
        mBorderLinePaint.setStrokeWidth(1.5f);
        mBorderLinePaint.setPathEffect(new DashPathEffect(new float[] { 3.0f, 2.0f }, 0.0f));
        mBorderLinePaint.setColor(Color.WHITE);
        mPlaybackLinePaint = new Paint();
        mPlaybackLinePaint.setAntiAlias(false);
        mPlaybackLinePaint.setColor(Color.RED);
        mTimecodePaint = new Paint();
        mTimecodePaint.setTextSize(12);
        mTimecodePaint.setAntiAlias(true);
        mTimecodePaint.setColor(Color.BLUE);
        mTimecodePaint.setShadowLayer(2, 1, 1, Color.GRAY);





        mSoundFile = null;
        mLenByZoomLevel = null;
        mValuesByZoomLevel = null;
        mHeightsAtThisZoomLevel = null;
        mOffset = 0;
        mPlaybackPos = -1;
        mSelectionStart = 0;
        mSelectionEnd = 0;
        mDensity = 1.0f;
        mInitialized = false;
    }



    public boolean hasSoundFile() {
        return mSoundFile != null;
    }

    public void setSoundFile(SoundFile soundFile) {
        mSoundFile = soundFile;
        mSampleRate = mSoundFile.getSampleRate();
        mSamplesPerFrame = mSoundFile.getSamplesPerFrame();
        computeDoublesForAllZoomLevels();
        mHeightsAtThisZoomLevel = null;
    }

    public boolean isInitialized() {
        return mInitialized;
    }



    public void setZoomLevel(int zoomLevel) {
        while (mZoomLevel > zoomLevel) {
            zoomIn();
        }
        while (mZoomLevel < zoomLevel) {
            zoomOut();
        }
    }

    public boolean canZoomIn() {
        return (mZoomLevel > 0);
    }

    public void zoomIn() {
        if (canZoomIn()) {
            mZoomLevel--;
            mSelectionStart *= 2;
            mSelectionEnd *= 2;
            mHeightsAtThisZoomLevel = null;
            int offsetCenter = mOffset + getMeasuredWidth() / 2;
            offsetCenter *= 2;
            mOffset = offsetCenter - getMeasuredWidth() / 2;
            if (mOffset < 0)
                mOffset = 0;
            invalidate();
        }
    }

    public boolean canZoomOut() {
        return (mZoomLevel < mNumZoomLevels - 1);
    }

    public void zoomOut() {
        if (canZoomOut()) {
            mZoomLevel++;
            mSelectionStart /= 2;
            mSelectionEnd /= 2;
            int offsetCenter = mOffset + getMeasuredWidth() / 2;
            offsetCenter /= 2;
            mOffset = offsetCenter - getMeasuredWidth() / 2;
            if (mOffset < 0)
                mOffset = 0;
            mHeightsAtThisZoomLevel = null;
            invalidate();
        }
    }

    public int maxPos() {
        return mLenByZoomLevel[mZoomLevel];
    }

    public int secondsToFrames(double seconds) {
        return (int)(1.0 * seconds * mSampleRate / mSamplesPerFrame + 0.5);
    }

    public int secondsToPixels(double seconds) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int)(z * seconds * mSampleRate / mSamplesPerFrame + 0.5);
    }

    public double pixelsToSeconds(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (pixels * (double)mSamplesPerFrame / (mSampleRate * z));
    }

    public int millisecsToPixels(int msecs) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int)((msecs * 1.0 * mSampleRate * z) /
                     (1000.0 * mSamplesPerFrame) + 0.5);
    }

    public int pixelsToMillisecs(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int)(pixels * (1000.0 * mSamplesPerFrame) /
                     (mSampleRate * z) + 0.5);
    }

    public void setParameters(int start, int end, int offset) {
        mSelectionStart = start;
        mSelectionEnd = end;
        mOffset = offset;
    }

    public int getStart() {
        return mSelectionStart;
    }

    public int getEnd() {
        return mSelectionEnd;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setPlayback(int pos) {
        mPlaybackPos = pos;
    }

    public void setListener(WaveformListener listener) {
        mListener = listener;
    }

    public void recomputeHeights(float density) {
        mHeightsAtThisZoomLevel = null;
        mDensity = density;
        mTimecodePaint.setTextSize((int)(12 * density));

        invalidate();
    }

    protected void drawWaveformLine(Canvas canvas,
                                    int x, int y0, int y1,
                                    Paint paint) {
        canvas.drawLine(x, y0, x, y1, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean test = true;

        if(test) {

            Log.e("XSXGOT","drawingnngng :: "+this.toString());
            canvas.drawCircle(0, 0, 50, teswtPaint);


            //return;
        }


        if (mSoundFile == null)
            return;

       if (mHeightsAtThisZoomLevel == null)
            computeIntsForThisZoomLevel();




        // Draw waveform
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int start = 0;
        int width = mHeightsAtThisZoomLevel.length - start;
        int ctr = measuredHeight / 2;

        if (width > measuredWidth)
            width = measuredWidth;

        // Draw grid
       /** double onePixelInSecs = pixelsToSeconds(1);
        boolean onlyEveryFiveSecs = (onePixelInSecs > 1.0 / 50.0);
        double fractionalSecs = mOffset * onePixelInSecs;
        int integerSecs = (int) fractionalSecs;
        int i = 0;
        while (i < width) {
            i++;
            fractionalSecs += onePixelInSecs;
            int integerSecsNew = (int) fractionalSecs;
            if (integerSecsNew != integerSecs) {
                integerSecs = integerSecsNew;
                if (!onlyEveryFiveSecs || 0 == (integerSecs % 5)) {
                    canvas.drawLine(i, 0, i, measuredHeight, mGridPaint);
                }
            }
        }**/
        int i = 0;
        // Draw waveform

        for (i = 0; i < width; i++) {
            Paint paint;
           // drawWaveformLine(canvas, i, 0, measuredHeight,mUnselectedBkgndLinePaint);
            paint = mUnselectedLinePaint;

            canvas.drawLine(i, ctr - mHeightsAtThisZoomLevel[start + i], i, ctr + 1 + mHeightsAtThisZoomLevel[start + i], paint);
           // Log.e("XSXGOTX","ctr - mHeightsAtThisZoomLevel[start + i] :: "+ (ctr - mHeightsAtThisZoomLevel[start + i]));
          //  Log.e("XSXGOTX","ctr + 1 + mHeightsAtThisZoomLevel[start + i]:: "+ (ctr + 1 + mHeightsAtThisZoomLevel[start + i]));
        }

        /**for (i = 0; i < width; i++) {
            Paint paint;
            if (i + start >= mSelectionStart &&
                i + start < mSelectionEnd) {
                paint = mSelectedLinePaint;
            } else {
                drawWaveformLine(canvas, i, 0, measuredHeight,
                                 mUnselectedBkgndLinePaint);
                paint = mUnselectedLinePaint;
            }
            drawWaveformLine(
                canvas, i,
                ctr - mHeightsAtThisZoomLevel[start + i],
                ctr + 1 + mHeightsAtThisZoomLevel[start + i],
                paint);

            if (i + start == mPlaybackPos) {
                canvas.drawLine(i, 0, i, measuredHeight, mPlaybackLinePaint);
            }
        }**/

        // If we can see the right edge of the waveform, draw the
        // non-waveform area to the right as unselected
       // for (i = width; i < measuredWidth; i++) {
           // drawWaveformLine(canvas, i, 0, measuredHeight,
                            // mUnselectedBkgndLinePaint);
       // }

        // Draw borders
        /**canvas.drawLine(
            mSelectionStart - mOffset + 0.5f, 30,
            mSelectionStart - mOffset + 0.5f, measuredHeight,
            mBorderLinePaint);
        canvas.drawLine(
            mSelectionEnd - mOffset + 0.5f, 0,
            mSelectionEnd - mOffset + 0.5f, measuredHeight - 30,
            mBorderLinePaint);**/

        // Draw timecode
       /** double timecodeIntervalSecs = 1.0;
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 5.0;
        }
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 15.0;
        }

        // Draw grid
        fractionalSecs = mOffset * onePixelInSecs;
        int integerTimecode = (int) (fractionalSecs / timecodeIntervalSecs);
        i = 0;
        while (i < width) {
            i++;
            fractionalSecs += onePixelInSecs;
            integerSecs = (int) fractionalSecs;
            int integerTimecodeNew = (int) (fractionalSecs /
                                            timecodeIntervalSecs);
            if (integerTimecodeNew != integerTimecode) {
                integerTimecode = integerTimecodeNew;

                // Turn, e.g. 67 seconds into "1:07"
                String timecodeMinutes = "" + (integerSecs / 60);
                String timecodeSeconds = "" + (integerSecs % 60);
                if ((integerSecs % 60) < 10) {
                    timecodeSeconds = "0" + timecodeSeconds;
                }
                String timecodeStr = timecodeMinutes + ":" + timecodeSeconds;
                float offset = (float) (
                    0.5 * mTimecodePaint.measureText(timecodeStr));
                canvas.drawText(timecodeStr,
                                i - offset,
                                (int)(12 * mDensity),
                                mTimecodePaint);
            }
        }

        if (mListener != null) {
            mListener.waveformDraw();
        }**/
    }

    /**
     * Called once when a new sound file is added
     */
    private void computeDoublesForAllZoomLevels() {


        Log.e("XSXGOT","computeDoublesForAllZoomLevels");
        int numFrames = mSoundFile.getNumFrames();
        int[] frameGains = mSoundFile.getFrameGains();
        double[] smoothedGains = new double[numFrames];
        if (numFrames == 1) {
            smoothedGains[0] = frameGains[0];
        } else if (numFrames == 2) {
            smoothedGains[0] = frameGains[0];
            smoothedGains[1] = frameGains[1];
        } else if (numFrames > 2) {
            smoothedGains[0] = (double)(
                (frameGains[0] / 2.0) +
                (frameGains[1] / 2.0));
            for (int i = 1; i < numFrames - 1; i++) {
                smoothedGains[i] = (double)(
                    (frameGains[i - 1] / 3.0) +
                    (frameGains[i    ] / 3.0) +
                    (frameGains[i + 1] / 3.0));
            }
            smoothedGains[numFrames - 1] = (double)(
                (frameGains[numFrames - 2] / 2.0) +
                (frameGains[numFrames - 1] / 2.0));
        }

        // Make sure the range is no more than 0 - 255
        double maxGain = 1.0;
        for (int i = 0; i < numFrames; i++) {
            if (smoothedGains[i] > maxGain) {
                maxGain = smoothedGains[i];
            }
        }
        double scaleFactor = 1.0;
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain;
        }

        // Build histogram of 256 bins and figure out the new scaled max
        maxGain = 0;
        int gainHist[] = new int[256];
        for (int i = 0; i < numFrames; i++) {
            int smoothedGain = (int)(smoothedGains[i] * scaleFactor);
            if (smoothedGain < 0)
                smoothedGain = 0;
            if (smoothedGain > 255)
                smoothedGain = 255;

            if (smoothedGain > maxGain)
                maxGain = smoothedGain;

            gainHist[smoothedGain]++;
        }

        // Re-calibrate the min to be 5%
        double minGain = 0;
        int sum = 0;
        while (minGain < 255 && sum < numFrames / 20) {
            sum += gainHist[(int)minGain];
            minGain++;
        }

        // Re-calibrate the max to be 99%
        sum = 0;
        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[(int)maxGain];
            maxGain--;
        }

        // Compute the heights
        double[] heights = new double[numFrames];
        double range = maxGain - minGain;
        for (int i = 0; i < numFrames; i++) {
            double value = (smoothedGains[i] * scaleFactor - minGain) / range;
            if (value < 0.0)
                value = 0.0;
            if (value > 1.0)
                value = 1.0;
            heights[i] = value * value;
        }

        mNumZoomLevels = 5;
        mLenByZoomLevel = new int[5];
        mZoomFactorByZoomLevel = new double[5];
        mValuesByZoomLevel = new double[5][];

        // Level 0 is doubled, with interpolated values
        mLenByZoomLevel[0] = numFrames * 2;
        mZoomFactorByZoomLevel[0] = 2.0;
        mValuesByZoomLevel[0] = new double[mLenByZoomLevel[0]];
        if (numFrames > 0) {
            mValuesByZoomLevel[0][0] = 0.5 * heights[0];
            mValuesByZoomLevel[0][1] = heights[0];
        }
        for (int i = 1; i < numFrames; i++) {
            mValuesByZoomLevel[0][2 * i] = 0.5 * (heights[i - 1] + heights[i]);
            mValuesByZoomLevel[0][2 * i + 1] = heights[i];
        }

        // Level 1 is normal
        mLenByZoomLevel[1] = numFrames;
        mValuesByZoomLevel[1] = new double[mLenByZoomLevel[1]];
        mZoomFactorByZoomLevel[1] = 1.0;
        for (int i = 0; i < mLenByZoomLevel[1]; i++) {
            mValuesByZoomLevel[1][i] = heights[i];
        }

        // 3 more levels are each halved
        for (int j = 2; j < 5; j++) {
            mLenByZoomLevel[j] = mLenByZoomLevel[j - 1] / 2;
            mValuesByZoomLevel[j] = new double[mLenByZoomLevel[j]];
            mZoomFactorByZoomLevel[j] = mZoomFactorByZoomLevel[j - 1] / 2.0;
            for (int i = 0; i < mLenByZoomLevel[j]; i++) {
                mValuesByZoomLevel[j][i] =
                    0.5 * (mValuesByZoomLevel[j - 1][2 * i] +
                           mValuesByZoomLevel[j - 1][2 * i + 1]);
            }
        }

        if (numFrames > 5000) {
            mZoomLevel = 3;
        } else if (numFrames > 1000) {
            mZoomLevel = 2;
        } else if (numFrames > 300) {
            mZoomLevel = 1;
        } else {
            mZoomLevel = 0;
        }

        mInitialized = true;
    }

    /**
     * Called the first time we need to draw when the zoom level has changed
     * or the screen is resized
     */
    private void computeIntsForThisZoomLevel() {
        int halfHeight = (getMeasuredHeight() / 2) - 1;
        mHeightsAtThisZoomLevel = new int[mLenByZoomLevel[mZoomLevel]];
        for (int i = 0; i < mLenByZoomLevel[mZoomLevel]; i++) {
            mHeightsAtThisZoomLevel[i] =
                (int)(mValuesByZoomLevel[mZoomLevel][i] * halfHeight);
        }
    }
}