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

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Inventory {

    private Map<String, SkuDetails> mSkuMap = new HashMap<>();
    private Map<String, Purchase> mPurchaseMap = new HashMap<>();

    public SkuDetails getSkuDetails(String sku) {
        return mSkuMap.get(sku);
    }

    public Purchase getPurchase(String sku) {
        return mPurchaseMap.get(sku);
    }

    public boolean hasPurchase(String sku) {
        return mPurchaseMap.containsKey(sku);
    }

    public boolean hasDetails(String sku) {
        return mSkuMap.containsKey(sku);
    }

    void addSkuDetails(SkuDetails details) {
        mSkuMap.put(details.sku, details);
    }

    void addPurchase(Purchase purchase) {
        mPurchaseMap.put(purchase.sku, purchase);
    }

}
