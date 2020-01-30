/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.wifitrackerlib;

import static android.net.wifi.WifiInfo.removeDoubleQuotes;

import static androidx.core.util.Preconditions.checkNotNull;

import static com.android.wifitrackerlib.Utils.getBestScanResultByLevel;
import static com.android.wifitrackerlib.Utils.getSecurityTypeFromWifiConfiguration;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

/**
 * WifiEntry representation of a Passpoint network, uniquely identified by FQDN.
 */
class PasspointWifiEntry extends WifiEntry {
    static final String KEY_PREFIX = "PasspointWifiEntry:";

    private final List<ScanResult> mCurrentHomeScanResults = new ArrayList<>();
    private final List<ScanResult> mCurrentRoamingScanResults = new ArrayList<>();

    @NonNull private final String mKey;
    @NonNull private String mFriendlyName;
    @NonNull private final Context mContext;
    @Nullable private PasspointConfiguration mPasspointConfig;
    @Nullable private WifiConfiguration mWifiConfig;
    private @Security int mSecurity;
    private boolean mIsRoaming = false;

    private int mLevel = WIFI_LEVEL_UNREACHABLE;
    protected long mSubscriptionExpirationTimeInMillis;

    /**
     * Create a PasspointWifiEntry with the associated PasspointConfiguration
     */
    PasspointWifiEntry(@NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull PasspointConfiguration passpointConfig,
            @NonNull WifiManager wifiManager) throws IllegalArgumentException {
        super(callbackHandler, wifiManager, false /* forSavedNetworksPage */);

        checkNotNull(passpointConfig, "Cannot construct with null PasspointConfiguration!");

        mContext = context;
        mPasspointConfig = passpointConfig;
        final HomeSp homeSp = passpointConfig.getHomeSp();
        mKey = fqdnToPasspointWifiEntryKey(homeSp.getFqdn());
        mFriendlyName = homeSp.getFriendlyName();
        mSecurity = SECURITY_NONE; //TODO: Should this always be Enterprise?
        mSubscriptionExpirationTimeInMillis =
                passpointConfig.getSubscriptionExpirationTimeInMillis();
    }

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    public String getTitle() {
        return mFriendlyName;
    }

    @Override
    public String getSummary() {
        return getSummary(true /* concise */);
    }

    @Override
    public String getSummary(boolean concise) {
        if (isExpired()) {
            return mContext.getString(R.string.wifi_passpoint_expired);
        }

        // TODO(b/70983952): Fill this method in
        return "Passpoint (Placeholder Text)"; // Placeholder string
    }

    @Override
    public int getLevel() {
        return mLevel;
    }

    @Override
    public String getSsid() {
        return mWifiConfig != null ? removeDoubleQuotes(mWifiConfig.SSID) : null;
    }

    @Override
    @Security
    public int getSecurity() {
        // TODO(b/70983952): Fill this method in
        return mSecurity;
    }

    @Override
    public String getMacAddress() {
        // TODO(b/70983952): Fill this method in
        return null;
    }

    @Override
    public boolean isMetered() {
        // TODO(b/70983952): Fill this method in
        return false;
    }

    @Override
    public boolean isSaved() {
        // TODO(b/70983952): Fill this method in
        return false;
    }

    @Override
    public boolean isSubscription() {
        return true;
    }

    @Override
    public WifiConfiguration getWifiConfiguration() {
        return null;
    }

    @Override
    public boolean canConnect() {
        return mLevel != WIFI_LEVEL_UNREACHABLE
                && getConnectedState() == CONNECTED_STATE_DISCONNECTED && mWifiConfig != null;
    }

    @Override
    public void connect(@Nullable ConnectCallback callback) {
        mConnectCallback = callback;

        if (mWifiConfig == null) {
            // We should not be able to call connect() if mWifiConfig is null
            new ConnectActionListener().onFailure(0);
        }
        mWifiManager.connect(mWifiConfig, new ConnectActionListener());
    }

    @Override
    public boolean canDisconnect() {
        return getConnectedState() == CONNECTED_STATE_CONNECTED;
    }

    @Override
    public void disconnect(@Nullable DisconnectCallback callback) {
        // TODO(b/70983952): Fill this method in
    }

    @Override
    public boolean canForget() {
        return true;
    }

    @Override
    public void forget(@Nullable ForgetCallback callback) {
        mForgetCallback = callback;
        mWifiManager.removePasspointConfiguration(mPasspointConfig.getHomeSp().getFqdn());
        new ForgetActionListener().onSuccess();
    }

    @Override
    public boolean canSignIn() {
        return false;
    }

    @Override
    public void signIn(@Nullable SignInCallback callback) {
        return;
    }

    @Override
    public boolean canShare() {
        return false;
    }

    @Override
    public boolean canEasyConnect() {
        return false;
    }

    @Override
    public String getQrCodeString() {
        return null;
    }

    @Override
    public boolean canSetPassword() {
        return false;
    }

    @Override
    public void setPassword(@NonNull String password) {
        // Do nothing.
    }

    @Override
    @MeteredChoice
    public int getMeteredChoice() {
        // TODO(b/70983952): Fill this method in
        return METERED_CHOICE_UNKNOWN;
    }

    @Override
    public boolean canSetMeteredChoice() {
        // TODO(b/70983952): Fill this method in
        return false;
    }

    @Override
    public void setMeteredChoice(int meteredChoice) {
        // TODO(b/70983952): Fill this method in
    }

    @Override
    public boolean canSetPrivacy() {
        // TODO(b/70983952): Fill this method in
        return false;
    }

    @Override
    @Privacy
    public int getPrivacy() {
        // TODO(b/70983952): Fill this method in
        return PRIVACY_UNKNOWN;
    }

    @Override
    public void setPrivacy(int privacy) {
        // TODO(b/70983952): Fill this method in
    }

    @Override
    public boolean isAutoJoinEnabled() {
        // TODO(b/70983952): Fill this method in
        return false;
    }

    @Override
    public boolean canSetAutoJoinEnabled() {
        // TODO(b/70983952): Fill this method in
        return false;
    }

    @Override
    public void setAutoJoinEnabled(boolean enabled) {
        // TODO(b/70983952): Fill this method in
    }

    @Override
    public String getSecurityString(boolean concise) {
        return concise ? mContext.getString(R.string.wifi_security_short_eap) :
                mContext.getString(R.string.wifi_security_eap);
    }

    @Override
    public boolean isExpired() {
        if (mSubscriptionExpirationTimeInMillis <= 0) {
            // Expiration time not specified.
            return false;
        } else {
            return System.currentTimeMillis() >= mSubscriptionExpirationTimeInMillis;
        }
    }

    @WorkerThread
    void updatePasspointConfig(@NonNull PasspointConfiguration passpointConfig) {
        checkNotNull(passpointConfig, "Cannot update with null PasspointConfiguration!");
        mPasspointConfig = passpointConfig;
        mFriendlyName = passpointConfig.getHomeSp().getFriendlyName();
        mSubscriptionExpirationTimeInMillis =
                passpointConfig.getSubscriptionExpirationTimeInMillis();
        notifyOnUpdated();
    }

    @WorkerThread
    void updateScanResultInfo(@NonNull WifiConfiguration wifiConfig,
            @Nullable List<ScanResult> homeScanResults,
            @Nullable List<ScanResult> roamingScanResults)
            throws IllegalArgumentException {
        mWifiConfig = wifiConfig;
        mSecurity = getSecurityTypeFromWifiConfiguration(wifiConfig);

        if (homeScanResults == null) {
            homeScanResults = new ArrayList<>();
        }
        if (roamingScanResults == null) {
            roamingScanResults = new ArrayList<>();
        }

        ScanResult bestScanResult;
        if (homeScanResults.isEmpty() && !roamingScanResults.isEmpty()) {
            mIsRoaming = true;
            bestScanResult = getBestScanResultByLevel(roamingScanResults);
        } else {
            mIsRoaming = false;
            bestScanResult = getBestScanResultByLevel(homeScanResults);
        }

        if (bestScanResult == null) {
            mLevel = WIFI_LEVEL_UNREACHABLE;
        } else {
            mLevel = mWifiManager.calculateSignalLevel(bestScanResult.level);
        }

        notifyOnUpdated();
    }

    @WorkerThread
    @Override
    protected boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo,
            @NonNull NetworkInfo networkInfo) {
        if (!wifiInfo.isPasspointAp()) {
            return false;
        }

        return TextUtils.equals(
                wifiInfo.getPasspointFqdn(), mPasspointConfig.getHomeSp().getFqdn());
    }

    @NonNull
    static String fqdnToPasspointWifiEntryKey(@NonNull String fqdn) {
        checkNotNull(fqdn, "Cannot create key with null fqdn!");
        return KEY_PREFIX + fqdn;
    }

    @Override
    String getScanResultDescription() {
        // TODO(b/70983952): Fill this method in.
        return "";
    }
}