/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.hammer.utils;

/**
 * The class encapsulating conference properties
 *
 * @author Maksym Kulish
 */
public class ConferenceInfo {

    /**
     * The "channelLastN" conference property
     * 
     */
    private String channelLastN;

    /**
     * The "adaptiveLastN" conference property
     */
    private String adaptiveLastN;

    /**
     * The "disableAdaptiveSimulcast" conference property
     */
    private String disableAdaptiveSimulcast;

    /**
     * The "openSctp" conference property
     */
    private String openSctp;

    /**
     * The "startAudioMuted" conference property
     */
    private String startAudioMuted;

    /**
     * The "startVideoMuted" conference property
     */
    private String startVideoMuted;

    /**
     * The "simulcastMode" conference property
     */
    private String simulcastMode;

    /**
     * The "enableLipSync" conference property
     */
    private String enableLipSync;

    /**
     * Create new ConferenceInfo instance
     * 
     * @param channelLastN The "channelLastN" conference property
     * @param adaptiveLastN The "adaptiveLastN" conference property
     * @param disableAdaptiveSimulcast The "disableAdaptiveSimulcast" conference property
     * @param openSctp The "openSctp" conference property
     * @param startAudioMuted The "startAudioMuted" conference property
     * @param startVideoMuted The "startVideoMuted" conference property
     * @param simulcastMode The "simulcastMode" conference property
     * @param enableLipSync The "enableLipSync" conference property
     */
    public ConferenceInfo(
            String channelLastN,
            String adaptiveLastN,
            String disableAdaptiveSimulcast,
            String openSctp,
            String startAudioMuted,
            String startVideoMuted,
            String simulcastMode,
            String enableLipSync)
    {
        this.channelLastN = channelLastN;
        this.adaptiveLastN = adaptiveLastN;
        this.disableAdaptiveSimulcast = disableAdaptiveSimulcast;
        this.openSctp = openSctp;
        this.startAudioMuted = startAudioMuted;
        this.startVideoMuted = startVideoMuted;
        this.simulcastMode = simulcastMode;
        this.enableLipSync = enableLipSync;
    }

    /**
     * Get the "adaptiveLastN" conference property
     * 
     * @return the "adaptiveLastN" conference property
     */
    public String getAdaptiveLastN() {
        return adaptiveLastN;
    }

    /**
     * Get the "channelLastN" conference property
     *
     * @return the "channelLastN" conference property
     */
    public String getChannelLastN() {
        return channelLastN;
    }

    /**
     * Get the "disableAdaptiveSimulcast" conference property
     *
     * @return the "disableAdaptiveSimulcast" conference property
     */
    public String getDisableAdaptiveSimulcast() {
        return disableAdaptiveSimulcast;
    }

    /**
     * Get the "openSctp" conference property
     *
     * @return the "openSctp" conference property
     */
    public String getOpenSctp() {
        return openSctp;
    }

    /**
     * Get the "startAudioMuted" conference property
     *
     * @return the "startAudioMuted" conference property
     */
    public String getStartAudioMuted() {
        return startAudioMuted;
    }

    /**
     * Get the "startVideoMuted" conference property
     *
     * @return the "startVideoMuted" conference property
     */
    public String getStartVideoMuted() {
        return startVideoMuted;
    }

    /**
     * Get the "simulcastMode" conference property
     *
     * @return the "simulcastMode" conference property
     */
    public String getSimulcastMode() {
        return simulcastMode;
    }

    /**
     * Get the "enableLipSync" conference property
     *
     * @return the "enableLipSync" conference property
     */
    public String getEnableLipSync() {
        return enableLipSync;
    }
}
