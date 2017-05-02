/*
 * Copyright (C) 2017 Nikola Kosev
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

package net.kosev.billing;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.security.SecureRandom;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class Billing {

    public static final int VERSION = 3;
    public static final String TYPE_INAPP = "inapp";
    public static final String TYPE_SUBS = "subs";

    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";

    public static final String GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST";
    public static final String GET_SKU_DETAILS_ITEM_TYPE_LIST = "ITEM_TYPE_LIST";

    // http://developer.android.com/google/play/billing/billing_reference.html
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
    public static final int BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;

    public static final int BILLING_UNKNOWN_ERROR = -1000;
    public static final int BILLING_DISPOSED = -1001;
    public static final int BILLING_VERIFICATION_FAILED = -1002;
    public static final int BILLING_PURCHASE_ERROR = -1003;

    private static final int STATE_INITIAL = 0;
    private static final int STATE_SETUP = 1;
    private static final int STATE_READY = 2;
    private static final int STATE_LOADING = 3;
    private static final int STATE_DESTROYED = 20;

    private volatile int mState = STATE_INITIAL;
    private Context mContext = null;
    private String mPublicKey = null;
    private IInAppBillingService mService = null;
    private ServiceConnection mServiceConn = null;
    private int mRequestCode;
    private PurchaseListener mPurchaseListener;
    private String mDeveloperPayload;

    public interface CreateListener {
        void onSuccess();
        void onError(int response, Exception e);
    }

    public interface InventoryListener {
        void onSuccess(Inventory inventory);
        void onError(int response, Exception e);
    }

    public interface PurchaseListener {
        void onSuccess(Purchase purchase);
        void onError(int response, Exception e);
    }

    public class BillingException extends Exception {
        private int mCode;

        public BillingException(int code) {
            mCode = code;
        }

        public int getCode() {
            return mCode;
        }
    }

    public Billing(Context context, String publicKey) {
        mContext = context;
        mPublicKey = publicKey;
    }

    public void create(final CreateListener listener) {
        if (mState != STATE_INITIAL) return;
        mState = STATE_SETUP;

        mServiceConn = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);

                try {
                    int response = mService.isBillingSupported(VERSION, mContext.getPackageName(), TYPE_INAPP);
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        if (listener != null) listener.onError(response, null);
                    } else {
                        mState = STATE_READY;
                        if (listener != null) listener.onSuccess();
                    }
                } catch (RemoteException e) {
                    if (listener != null) listener.onError(BILLING_RESPONSE_RESULT_ERROR, e);
                }
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");

        try {
            if (!mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE)) {
                if (listener != null) listener.onError(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, null);
            }
        } catch (SecurityException e) {
            if (listener != null) listener.onError(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, e);
        }
    }

    public void destroy() {
        try {
            mContext.unbindService(mServiceConn);
        } catch (Exception ignored) { }
        mServiceConn = null;
        mService = null;
        mState = STATE_DESTROYED;
    }

    public void loadInventory(ArrayList<String> skus, InventoryListener listener) {
        if (mState != STATE_READY) return;
        mState = STATE_LOADING;

        LoadInventoryTask task = new LoadInventoryTask();
        task.execute(skus, listener);
    }

    public void launchPurchaseFlow(Activity activity, String sku, int requestCode, PurchaseListener listener) {
        if (mState != STATE_READY) return;
        mState = STATE_LOADING;
        mPurchaseListener = null;

        try {
            mDeveloperPayload = generateDeveloperPayload();
            Bundle buyIntentBundle = mService.getBuyIntent(VERSION, mContext.getPackageName(), sku, TYPE_INAPP, mDeveloperPayload);
            int response = getResponseCodeFromBundle(buyIntentBundle);
            if (response == BILLING_RESPONSE_RESULT_OK) {
                PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
                mRequestCode = requestCode;
                mPurchaseListener = listener;
                //noinspection ConstantConditions
                activity.startIntentSenderForResult(pendingIntent.getIntentSender(), requestCode, new Intent(), 0, 0, 0);
            } else {
                mState = STATE_READY;
                if (listener != null) listener.onError(response, null);
            }
        } catch (Exception e) {
            mState = STATE_READY;
            if (listener != null) listener.onError(BILLING_PURCHASE_ERROR, e);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != mRequestCode) return;

        mState = STATE_READY;
        PurchaseListener listener = mPurchaseListener;

        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                int response = getResponseCodeFromBundle(data.getExtras());
                if (response == BILLING_RESPONSE_RESULT_OK) {
                    String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
                    String signature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);
                    if (purchaseData != null && signature != null) {
                        try {
                            Purchase purchase = new Purchase(purchaseData, signature);
                            final boolean isVerified = Security.verifyPurchase(mPublicKey, purchaseData, signature) && purchase.developerPayload.equals(mDeveloperPayload);
                            if (isVerified) {
                                if (listener != null) listener.onSuccess(purchase);
                            } else {
                                if (listener != null) listener.onError(BILLING_VERIFICATION_FAILED, null);
                            }
                        } catch (JSONException e) {
                            if (listener != null) listener.onError(BILLING_PURCHASE_ERROR, e);
                        }
                    } else {
                        if (listener != null) listener.onError(BILLING_PURCHASE_ERROR, null);
                    }
                } else {
                    if (listener != null) listener.onError(response, null);
                }
            } else {
                if (listener != null) listener.onError(BILLING_PURCHASE_ERROR, null);
            }
        }
    }

    private void getSkuDetails(Inventory inventory, ArrayList<String> skus) throws RemoteException, JSONException, BillingException {
        if (mContext == null || mService == null) {
            throw new BillingException(BILLING_DISPOSED);
        }

        Bundle skuParams = new Bundle();
        skuParams.putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, skus);

        Bundle skuResult = mService.getSkuDetails(VERSION, mContext.getPackageName(), TYPE_INAPP, skuParams);

        int response = getResponseCodeFromBundle(skuResult);
        if (response == BILLING_RESPONSE_RESULT_OK) {
            ArrayList<String> items = skuResult.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST);

            if (items != null) {
                for (String item : items) {
                    SkuDetails sku = new SkuDetails(item);
                    inventory.addSkuDetails(sku);
                }
            }
        } else {
            throw new BillingException(response);
        }
    }

    private void getPurchases(Inventory inventory) throws RemoteException, JSONException, BillingException {
        if (mContext == null || mService == null) {
            throw new BillingException(BILLING_DISPOSED);
        }

        Bundle ownedItems = mService.getPurchases(VERSION, mContext.getPackageName(), TYPE_INAPP, null);

        int response = getResponseCodeFromBundle(ownedItems);
        if (response == BILLING_RESPONSE_RESULT_OK) {
            ArrayList<String> ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
            ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);

            if (ownedSkus != null && purchaseDataList != null && signatureList != null) {
                for (int i = 0; i < purchaseDataList.size(); i++) {
                    String purchaseData = purchaseDataList.get(i);
                    String signature = signatureList.get(i);
                    //String sku = ownedSkus.get(i);

                    if (Security.verifyPurchase(mPublicKey, purchaseData, signature)) {
                        Purchase purchase = new Purchase(purchaseData, signature);
                        inventory.addPurchase(purchase);
                    } else {
                        throw new BillingException(BILLING_VERIFICATION_FAILED);
                    }
                }
            }
        } else {
            throw new BillingException(response);
        }
    }

    private static int getResponseCodeFromBundle(Bundle bundle) {
        Object value = bundle.get(RESPONSE_CODE);
        if (value == null) {
            return BILLING_RESPONSE_RESULT_OK;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return (int)((Long)value).longValue();
        } else {
            throw new RuntimeException("Unexpected type for bundle response code: " + value.getClass().getName());
        }
    }

    private static String generateDeveloperPayload() {
        SecureRandom random = new SecureRandom();
        byte[] output = new byte[16];
        random.nextBytes(output);

        return Base64.encode(output);
    }

    private class LoadInventoryTask extends AsyncTask<Object, Void, Inventory> {
        private InventoryListener mListener;
        private int mResponse = BILLING_UNKNOWN_ERROR;
        private Exception mException;

        protected Inventory doInBackground(Object... params) {
            try {
                mListener = (InventoryListener) params[1];
                @SuppressWarnings("unchecked")
                ArrayList<String> skus = (ArrayList<String>) params[0];
                Inventory result = new Inventory();
                getSkuDetails(result, skus);
                getPurchases(result);
                return result;
            } catch (BillingException e) {
                mException = e;
                mResponse = e.getCode();
                cancel(true);
                return null;
            } catch (Exception e) {
                mException = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Inventory result) {
            super.onPostExecute(result);
            mState = STATE_READY;
            if (!isCancelled()) {
                if (mListener != null) mListener.onSuccess(result);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mState = STATE_READY;
            if (mListener != null) mListener.onError(mResponse, mException);
        }
    }

}
