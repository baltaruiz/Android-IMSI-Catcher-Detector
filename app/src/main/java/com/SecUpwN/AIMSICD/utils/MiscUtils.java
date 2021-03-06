package com.SecUpwN.AIMSICD.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.SecUpwN.AIMSICD.AIMSICD;
import com.SecUpwN.AIMSICD.R;
import com.SecUpwN.AIMSICD.activities.CustomPopUp;
import com.SecUpwN.AIMSICD.smsdetection.SmsDetectionDbAccess;
import com.SecUpwN.AIMSICD.smsdetection.SmsDetectionDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Paul Kinsella on 04/03/15.
 * paulkinsella29@yahoo.ie
 */

public class MiscUtils {

    public static String setAssetsString(Context context){
        BufferedReader reader = null;
        StringBuilder buildassets = new StringBuilder();
        try{
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open("CREDITS")));
            String rline = reader.readLine().replace("'","\\'").replace("\\n","");

            while (rline != null ){
                buildassets.append(rline).append("\n");
                rline = reader.readLine().replace("'","\\'").replace("\\n","");
            }
        } catch (Exception ee){
            ee.printStackTrace();
        }finally {
            if(reader != null){
                try {
                    reader.close();
                } catch (Exception ee){
                    ee.printStackTrace();
                }
            }
        }

        return buildassets.toString();
    }

    public static void startPopUpInfo(Context context,int mode){
        Intent i = new Intent(context, CustomPopUp.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("display_mode",mode);
        context.startActivity(i);
    }

    public static String getCurrentTimeStamp(){

        Date now = new Date();
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(now);
        return timestamp;
    }

    /*
      Call This function from any activity to set notification:
      Example:
                          MiscUtils.showNotification(getApplicationContext(),
                          getResources().getString(R.string.app_name_short),
                          getResources().getString(R.string.app_name_short)+" - "+getResources().getString(R.string.status_good)                            ,
                          R.drawable.sense_ok,false);
   */
    public static void showNotification(Context context ,String tickertext,String contentText, int drawable_id,boolean auto_cancel){
        int NOTIFICATION_ID = 1;

        Intent notificationIntent = new Intent(context, AIMSICD.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_FROM_BACKGROUND);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context, NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(drawable_id)
                        .setTicker(tickertext)
                        .setContentTitle(context.getResources().getString(R.string.main_app_name))
                        .setContentText(contentText)
                        .setOngoing(true)
                        .setAutoCancel(auto_cancel)
                        .setContentIntent(contentIntent)
                        .build();
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder);

    }

    /*
         Coder:banjaxbanjo
         All new database detection strings will be added here so we
         don't need to keep updating db every time we find a new string.

         to add a new string in det_strings.json see example below:

         {"detection_string":"incoming msg. Mti 0 ProtocolID 0 DCS 0x04 class -1",
         "detection_type":"WAPPUSH"}

      */
    public static void refreshDetectionDbStrings(Context con){
        SmsDetectionDbAccess dbaccess = new SmsDetectionDbAccess(con);
        BufferedReader reader = null;
        StringBuilder json_file = new StringBuilder();
        try{
            reader = new BufferedReader(new InputStreamReader(con.getAssets().open("det_strings.json")));
            String rline = reader.readLine();

            while (rline != null ){
                json_file.append(rline);
                rline = reader.readLine();
            }
            Log.i("refreshDetectionDbStrings", json_file.toString());
        } catch (Exception ee){
            ee.printStackTrace();
        }finally {
            if(reader != null){
                try {
                    reader.close();
                } catch (Exception ee){
                    ee.printStackTrace();
                }
            }
        }

        JSONObject json_response;

        try {

            json_response = new JSONObject(json_file.toString());
            JSONArray json_array_node = json_response.optJSONArray("load_detection_strings");

            int json_array_len = json_array_node.length();

            for(int i=0; i < json_array_len; i++)
            {
                dbaccess.open();
                JSONObject current_json_object = json_array_node.getJSONObject(i);
                ContentValues store_new_det_string = new ContentValues();
                store_new_det_string.put(SmsDetectionDbHelper.SILENT_SMS_STRING_COLUMN,
                        current_json_object.optString("detection_string").toString());
                store_new_det_string.put(SmsDetectionDbHelper.SILENT_SMS_TYPE_COLUMN,
                        current_json_object.optString("detection_type").toString());
                if(dbaccess.insertNewDetectionString(store_new_det_string)){
                    Log.i("refreshDetectionDbStrings",">>>String added success");
                }
                dbaccess.close();

            }

        } catch (JSONException e) {
            dbaccess.close();
            Log.e("refreshDetectionDbStrings",">>> Error parsing JsonFile "+e.toString());
            e.printStackTrace();
        }

    }
}
