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

public class SkuDetails {

    public String sku;
    public String type;
    public String price;
    public int priceAmountMicros;
    public String priceCurrencyCode;
    public String title;
    public String description;

    SkuDetails(String jsonSkuDetails) throws JSONException {
        JSONObject json = new JSONObject(jsonSkuDetails);
        sku = json.optString("productId");
        type = json.optString("type");
        price = json.optString("price");
        priceAmountMicros = json.optInt("price_amount_micros");
        priceCurrencyCode = json.optString("price_currency_code");
        title = json.optString("title");
        description = json.optString("description");
    }

}
