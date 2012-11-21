/*
 * Copyright (C) 2012 Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo;

import android.media.AudioManager;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Contains methods to handle registering/unregistering remote control clients.
 * These methods only run on ICS devices. On previous devices, all methods are
 * no-ops.
 */
@SuppressWarnings({
    "rawtypes"
})
public class RemoteControlHelper {
    private static final String TAG = "RemoteControlHelper";

    private static boolean sHasRemoteControlAPIs = false;

    private static Method sRegisterRemoteControlClientMethod;

    private static Method sUnregisterRemoteControlClientMethod;

    static {
        try {
            final ClassLoader classLoader = RemoteControlHelper.class.getClassLoader();
            final Class sRemoteControlClientClass = RemoteControlClientCompat
                    .getActualRemoteControlClientClass(classLoader);
            sRegisterRemoteControlClientMethod = AudioManager.class.getMethod(
                    "registerRemoteControlClient", new Class[] {
                        sRemoteControlClientClass
                    });
            sUnregisterRemoteControlClientMethod = AudioManager.class.getMethod(
                    "unregisterRemoteControlClient", new Class[] {
                        sRemoteControlClientClass
                    });
            sHasRemoteControlAPIs = true;
        } catch (final ClassNotFoundException e) {
            // Silently fail when running on an OS before ICS.
        } catch (final NoSuchMethodException e) {
            // Silently fail when running on an OS before ICS.
        } catch (final IllegalArgumentException e) {
            // Silently fail when running on an OS before ICS.
        } catch (final SecurityException e) {
            // Silently fail when running on an OS before ICS.
        }
    }

    public static void registerRemoteControlClient(final AudioManager audioManager,
            final RemoteControlClientCompat remoteControlClient) {
        if (!sHasRemoteControlAPIs) {
            return;
        }

        try {
            sRegisterRemoteControlClientMethod.invoke(audioManager,
                    remoteControlClient.getActualRemoteControlClientObject());
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static void unregisterRemoteControlClient(final AudioManager audioManager,
            final RemoteControlClientCompat remoteControlClient) {
        if (!sHasRemoteControlAPIs) {
            return;
        }

        try {
            sUnregisterRemoteControlClientMethod.invoke(audioManager,
                    remoteControlClient.getActualRemoteControlClientObject());
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
