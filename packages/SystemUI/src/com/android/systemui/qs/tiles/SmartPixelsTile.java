/*
 * Copyright (C) 2019 crDroid Android Project
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

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

import javax.inject.Inject;

public class SmartPixelsTile extends QSTileImpl<BooleanState> {
    private static final ComponentName SMART_PIXELS_SETTING_COMPONENT = new ComponentName(
            "com.android.settings", "com.android.settings.Settings$SmartPixelsActivity");

    private static final Intent SMART_PIXELS_SETTINGS =
            new Intent().setComponent(SMART_PIXELS_SETTING_COMPONENT);

    private boolean mSmartPixelsEnable;
    private boolean mSmartPixelsOnPowerSave;
    private boolean mLowPowerMode;
    private boolean mListening;

    @Inject
    public SmartPixelsTile(QSHost host) {
        super(host);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {
        mListening = listening;
        if (mListening) {
            mContext.registerReceiver(mSmartPixelsReceiver,
                    new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        } else {
            mContext.unregisterReceiver(mSmartPixelsReceiver);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void handleClick() {
        mSmartPixelsEnable = (Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE,
                0, UserHandle.USER_CURRENT) == 1);
        mSmartPixelsOnPowerSave = (Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.SMART_PIXELS_ON_POWER_SAVE,
                0, UserHandle.USER_CURRENT) == 1);
        mLowPowerMode = (Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.LOW_POWER_MODE, 0) == 1);
        if (mLowPowerMode && mSmartPixelsOnPowerSave) {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SMART_PIXELS_ON_POWER_SAVE,
                    0, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SMART_PIXELS_ENABLE,
                    0, UserHandle.USER_CURRENT);
        } else if (!mSmartPixelsEnable) {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SMART_PIXELS_ENABLE,
                    1, UserHandle.USER_CURRENT);
        } else {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SMART_PIXELS_ENABLE,
                    0, UserHandle.USER_CURRENT);
        }
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return SMART_PIXELS_SETTINGS;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        mSmartPixelsEnable = (Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE,
                0, UserHandle.USER_CURRENT) == 1);
        mSmartPixelsOnPowerSave = (Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.SMART_PIXELS_ON_POWER_SAVE,
                0, UserHandle.USER_CURRENT) == 1);
        mLowPowerMode = (Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.LOW_POWER_MODE, 0) == 1);
        state.icon  = ResourceIcon.get(R.drawable.ic_qs_smart_pixels);
        if (mLowPowerMode && mSmartPixelsOnPowerSave) {
            state.label = mContext.getString(R.string.quick_settings_smart_pixels_on_power_save);
            state.value = true;
        } else if (mSmartPixelsEnable) {
            state.label = mContext.getString(R.string.quick_settings_smart_pixels);
            state.value = true;
        } else {
            state.label = mContext.getString(R.string.quick_settings_smart_pixels);
            state.value = false;
        }
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_smart_pixels);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LINEAGE;
    }

    private BroadcastReceiver mSmartPixelsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshState();
        }
    };
}
