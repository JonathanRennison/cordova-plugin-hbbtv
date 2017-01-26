/*******************************************************************************
 *
 * Copyright (c) 2015 Fraunhofer FOKUS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
 *
 * AUTHORS: Louay Bassbouss (louay.bassbouss@fokus.fraunhofer.de)
 *
 ******************************************************************************/
package de.fhg.fokus.famium.hbbtv;

import android.util.Log;
import android.os.Handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.fhg.fokus.famium.hbbtv.dial.Dial;
import de.fhg.fokus.famium.hbbtv.dial.DialAppInfo;
import de.fhg.fokus.famium.hbbtv.dial.DialDevice;

/**
 * Created by lba on 23/04/15.
 */
public class HbbTvManager{
  public static final String TAG = "HbbTvManager";
  public static final int TIMEOUT = 5000;
  private Dial mDial;
  private Map<String, DialAppInfo> mHbbTvTerminals;
  private DiscoverTerminalsCallback mDiscoverTerminalsCallback;
  private DiscoverTerminalImmediateCallback mDiscoverTerminalImmediateCallback;
  private boolean searching = false;
  private Handler mHandler;
  public HbbTvManager(){

  };

  public HbbTvManager(DiscoverTerminalsCallback discoverTerminalsCallback){
    mDiscoverTerminalsCallback = discoverTerminalsCallback;
    mHandler = new Handler();
  }

  public DiscoverTerminalsCallback getDiscoverTerminalsCallback() {
    return mDiscoverTerminalsCallback;
  }

  public void setDiscoverTerminalsCallback(DiscoverTerminalsCallback discoverTerminalsCallback) {
    this.mDiscoverTerminalsCallback = discoverTerminalsCallback;
  }

  public DiscoverTerminalImmediateCallback getDiscoverTerminalImmediateCallback() {
    return mDiscoverTerminalImmediateCallback;
  }

  public void setDiscoverTerminalImmediateCallback(DiscoverTerminalImmediateCallback discoverTerminalImmediateCallback) {
    this.mDiscoverTerminalImmediateCallback = discoverTerminalImmediateCallback;
  }

  public synchronized void discoverTerminals(){
    if(!searching){
      searching = true;
      Log.d(TAG, "discoverTerminals: start searching");
      try {
        getHbbTvTerminals().clear();
        getDial().search(TIMEOUT);
      }
      catch (IOException e){
        Log.e(TAG,e.getMessage(),e);
      }
      mHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          synchronized(HbbTvManager.this) {
            Log.d(TAG, "discoverTerminals: stop searching");
            try {
              getDial().cancel();
            }
            catch (IOException e){
              Log.e(TAG,e.getMessage(),e);
            }
            if (getDiscoverTerminalsCallback() != null){
              getDiscoverTerminalsCallback().onDiscoverTerminals(getLastFoundTerminals());
            }
            searching = false;
          }
        }
      }, TIMEOUT);
    }
  }

  public Map<String, DialAppInfo> getLastFoundTerminals(){
    return new HashMap<String, DialAppInfo>(getHbbTvTerminals());
  };

  private synchronized Map<String, DialAppInfo> getHbbTvTerminals() {
    if(mHbbTvTerminals == null ){
      mHbbTvTerminals = new HashMap<String, DialAppInfo>();
    }
    return mHbbTvTerminals;
  }

  private synchronized  Dial getDial(){
    if(mDial == null){
      mDial = new Dial(new Dial.DeviceFoundCallback() {
        @Override
        public void onDialDeviceFound(final DialDevice dialDevice) {
          Log.d(TAG, "onDialDeviceFound: " + dialDevice.getApplicationUrl());
          dialDevice.getAppInfo("HbbTV",new Dial.GetAppInfoCallback() {
            @Override
            public void onReceiveAppInfo(DialAppInfo appInfo) {
              Log.d(TAG, "onReceiveAppInfo: " + dialDevice.getApplicationUrl() + ", " + appInfo);
              if(appInfo != null /*&& appInfo.getAdditionalData("X_HbbTV_App2AppURL") != null*/){
                getHbbTvTerminals().put(dialDevice.getApplicationUrl(),appInfo);
                if(getDiscoverTerminalImmediateCallback() != null) {
                  getDiscoverTerminalImmediateCallback().onDiscoverTerminalImmediate(appInfo);
                }
              }
            }
          });
        }
      });
    }
    return mDial;
  }

  public interface DiscoverTerminalsCallback {
    public void onDiscoverTerminals(Map<String,DialAppInfo> terminals);
  }

  public interface DiscoverTerminalImmediateCallback {
    public void onDiscoverTerminalImmediate(DialAppInfo terminal);
  }
}
