/*
 * Copyright (C) 2008 The Guava Authors Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.utils;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.TargetApi;

import java.util.concurrent.TimeUnit;

/**
 * An object that measures elapsed time in nanoseconds. It is useful to measure
 * elapsed time using this class instead of direct calls to
 * {@link System#nanoTime} for a few reasons:
 * <ul>
 * <li>An alternate time source can be substituted, for testing or performance
 * reasons.
 * <li>As documented by {@code nanoTime}, the value returned has no absolute
 * meaning, and can only be interpreted as relative to another timestamp
 * returned by {@code nanoTime} at a different time. {@code Stopwatch} is a more
 * effective abstraction because it exposes only these relative values, not the
 * absolute ones.
 * </ul>
 * <p>
 * Basic usage:
 * 
 * <pre>
 *   Stopwatch stopwatch = new Stopwatch().{@link #start start}();
 *   doSomething();
 *   stopwatch.{@link #stop stop}(); // optional
 * 
 *   long millis = stopwatch.{@link #elapsedMillis elapsedMillis}();
 * 
 *   log.info("that took: " + stopwatch); // formatted string like "12.3 ms"
 * </pre>
 * <p>
 * Stopwatch methods are not idempotent; it is an error to start or stop a
 * stopwatch that is already in the desired state.
 * <p>
 * When testing code that uses this class, use the
 * {@linkplain #Stopwatch(Ticker) alternate constructor} to supply a fake or
 * mock ticker. <!-- TODO(kevinb): restore the "such as" --> This allows you to
 * simulate any valid behavior of the stopwatch.
 * <p>
 * <b>Note:</b> This class is not thread-safe.
 * 
 * @author Kevin Bourrillion
 * @since 10.0
 */
@TargetApi(9)
public final class Stopwatch {

    private final Ticker mTicker;

    private boolean mIsRunning;

    private long mElapsedNanos;

    private long mStartTick;

    /**
     * Creates (but does not start) a new stopwatch using
     * {@link System#nanoTime} as its time source.
     */
    public Stopwatch() {
        this(Ticker.systemTicker());
    }

    /**
     * Creates (but does not start) a new stopwatch, using the specified time
     * source.
     */
    public Stopwatch(final Ticker ticker) {
        mTicker = checkNotNull(ticker);
    }

    /**
     * Returns {@code true} if {@link #start()} has been called on this
     * stopwatch, and {@link #stop()} has not been called since the last call to
     * {@code start()}.
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * Starts the stopwatch.
     * 
     * @return this {@code Stopwatch} instance
     * @throws IllegalStateException if the stopwatch is already running.
     */
    public Stopwatch start() {
        checkState(!mIsRunning);
        mIsRunning = true;
        mStartTick = mTicker.read();
        return this;
    }

    /**
     * Stops the stopwatch. Future reads will return the fixed duration that had
     * elapsed up to this point.
     * 
     * @return this {@code Stopwatch} instance
     * @throws IllegalStateException if the stopwatch is already stopped.
     */
    public Stopwatch stop() {
        final long mTick = mTicker.read();
        checkState(mIsRunning);
        mIsRunning = false;
        mElapsedNanos += mTick - mStartTick;
        return this;
    }

    /**
     * Sets the elapsed time for this stopwatch to zero, and places it in a
     * stopped state.
     * 
     * @return this {@code Stopwatch} instance
     */
    public Stopwatch reset() {
        mElapsedNanos = 0;
        mIsRunning = false;
        return this;
    }

    private long elapsedNanos() {
        return mIsRunning ? mTicker.read() - mStartTick + mElapsedNanos : mElapsedNanos;
    }

    /**
     * Returns the current elapsed time shown on this stopwatch, expressed in
     * the desired time unit, with any fraction rounded down.
     * <p>
     * Note that the overhead of measurement can be more than a microsecond, so
     * it is generally not useful to specify {@link TimeUnit#NANOSECONDS}
     * precision here.
     */
    public long elapsedTime(final TimeUnit desiredUnit) {
        return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
    }

    /**
     * Returns the current elapsed time shown on this stopwatch, expressed in
     * milliseconds, with any fraction rounded down. This is identical to
     * {@code elapsedTime(TimeUnit.MILLISECONDS)}.
     */
    public long elapsedMillis() {
        return elapsedTime(MILLISECONDS);
    }

    /**
     * Returns a string representation of the current elapsed time.
     */
    @Override
    public String toString() {
        return toString(4);
    }

    /**
     * Returns a string representation of the current elapsed time, choosing an
     * appropriate unit and using the specified number of significant figures.
     * For example, at the instant when {@code elapsedTime(NANOSECONDS)} would
     * return {1234567}, {@code toString(4)} returns {@code "1.235 ms"}.
     * 
     * @deprecated Use {@link #toString()} instead. This method is scheduled to
     *             be removed in Guava release 15.0.
     */
    @Deprecated
    public String toString(final int significantDigits) {
        final long mNanos = elapsedNanos();

        final TimeUnit mUnit = chooseUnit(mNanos);
        final double mValue = (double)mNanos / NANOSECONDS.convert(1, mUnit);

        /* Too bad this functionality is not exposed as a regular method call */
        return String.format("%." + significantDigits + "g %s", mValue, abbreviate(mUnit));
    }

    private static TimeUnit chooseUnit(final long nanos) {
        if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
            return SECONDS;
        }
        if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MILLISECONDS;
        }
        if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    private static String abbreviate(final TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "\u03bcs";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            default:
                throw new AssertionError();
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling
     * instance, but not involving any parameters to the calling method.
     * 
     * @param expression a boolean expression
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(final boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling
     * method is not null.
     * 
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(final T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

}
