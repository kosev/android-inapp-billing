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

import org.json.JSONException;
import org.json.JSONObject;

public class Purchase {

    public String sku;
    public String orderId;
    public String packageName;
    public long purchaseTime;
    public int purchaseState;
    public String developerPayload;
    public String purchaseToken;
    public String signature;

    Purchase(String jsonPurchase, String signature) throws JSONException {
        JSONObject json = new JSONObject(jsonPurchase);
        sku = json.optString("productId");
        orderId = json.optString("orderId");
        packageName = json.optString("packageName");
        purchaseTime = json.optLong("purchaseTime");
        purchaseState = json.optInt("purchaseState");
        developerPayload = json.optString("developerPayload");
        purchaseToken = json.optString("token", json.optString("purchaseToken"));
        this.signature = signature;
    }

}
