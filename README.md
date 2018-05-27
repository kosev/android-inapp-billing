# Android In-app Billing API Version 3
Improved version of Google's in-app purchase helper classes. All concurrency problems have been eliminated. In-app products and subscriptions are supported, but consuming is still not.

## Installation
In your project level build.gradle:

```java
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

In your app level build.gradle:

```java
dependencies {
    implementation 'com.github.kosev:android-inapp-billing:1.1.2'
}
```

No need to include the BILLING permission and custom ProGuard rulers, they will be merged with your settings.

## Usage
Init billing and query purchases in your activity:

```java
private Billing mBilling;
...
mBilling = new Billing(this, "Your deobfuscated public key here");
mBilling.create(new Billing.CreateListener() {
    public void onSuccess() {
        ArrayList<String> skus = new ArrayList<>();
        skus.add("SKU to query");
        mBilling.loadInventory(skus, new Billing.InventoryListener() {
            public void onSuccess(Inventory inventory) {
                // inventory.hasPurchase("Your SKU");
            }

            public void onError(int response, Exception e) {
                // show error
            }
        });
    }

    public void onError(int response, Exception e) {
        // billing is not supported
    }
});
```

To make a purchase, include activity result handler and then launch purchase flow for your product's SKU.
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (mBilling != null) {
        mBilling.onActivityResult(requestCode, resultCode, data);
    }
}
...
mBilling.launchPurchaseFlow(SettingsActivity.this, "Your SKU", Billing.TYPE_INAPP, REQUEST_PURCHASE,
    new Billing.PurchaseListener() {
        public void onSuccess(Purchase purchase) {
            if ("Your SKU".equals(purchase.sku)) {
                // complete order
            }
        }

        public void onError(int response, Exception e) {
            // show error
        }
    });
```

Releasing resources
```java
@Override
protected void onDestroy() {
    if (mBilling != null) {
        mBilling.destroy();
        mBilling = null;
    }

    super.onDestroy();
}
```

## License
```
Copyright 2017 Nikola Kosev

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
