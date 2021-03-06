package com.stockhawk.Utils;

import android.content.ContentProviderOperation;
import android.text.TextUtils;
import android.util.Log;

import com.stockhawk.Constants;
import com.stockhawk.model.QuoteColumns;
import com.stockhawk.model.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;
    private static String KEY_CHANGE = "Change";
    private static String KEY_SYMBOL = "symbol";
    private static String KEY_BID = "Bid";
    private static String KEY_CHANGE_PERCENT = "ChangeinPercent";

    public static ArrayList quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(Constants.QUERY);
                int count = Integer.parseInt(jsonObject.getString(Constants.COUNT));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(Constants.RESULT)
                            .getJSONObject(Constants.QUOTE);
                    ContentProviderOperation operation = buildBatchOperation(jsonObject);
                    if(operation!=null) {
                        batchOperations.add(operation);
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject(Constants.RESULT).getJSONArray(Constants.QUOTE);

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            ContentProviderOperation operation = buildBatchOperation(jsonObject);
                            if(operation!=null) {
                                batchOperations.add(operation);
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        if (!TextUtils.isEmpty(bidPrice)) {
            bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
            return bidPrice;
        } else {
            return "";
        }
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";;
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        String changes = null;
        try {
            changes = jsonObject.getString(KEY_CHANGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("values", changes);
        if (changes != null && changes != Constants.NULL) {
            try {
                String change = jsonObject.getString(KEY_CHANGE);
                builder.withValue(QuoteColumns.SYMBOL, getKeyValue(jsonObject, KEY_SYMBOL));
                builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(getKeyValue(jsonObject, KEY_BID)));
                builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                        getKeyValue(jsonObject, KEY_CHANGE_PERCENT), true));
                builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
                builder.withValue(QuoteColumns.ISCURRENT, 1);
                if (change.charAt(0) == '-') {
                    builder.withValue(QuoteColumns.ISUP, 0);
                } else {
                    builder.withValue(QuoteColumns.ISUP, 1);
                }
                return builder.build();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getKeyValue(JSONObject object, String key){
        String value = "";
        try {
            value = object.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        if (value != null && value != Constants.NULL) {
            return value;
        }else{
            return null;
        }
    }
}
