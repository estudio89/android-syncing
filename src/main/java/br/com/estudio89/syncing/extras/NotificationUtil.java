package br.com.estudio89.syncing.extras;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.models.SyncModel;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by luccascorrea on 5/15/15.
 *
 */
public class NotificationUtil {
    private static final String SHOWN_NOTIFICATION_COUNT = "shown_notification_count";
    private static final String FOREGROUND_KEY = "foreground";
    private static String LAST_NOTIFICATION_KEY = "last_notification";

    public interface NotificationGenerator {
        /**
         * Indicates if a notification should be shown
         *
         * @param context context
         * @param item item that could generate a notification
         *
         * @return This method MUST return an array with 3 items:
         * - Item 0: Boolean indicating if notification should be shown
         * - Item 1: title of notification
         * - Item 2: text of notification
         * - Item 3: id of object that originated the notification
         */
        Object[] shouldDisplayNotification(Context context, SyncModel item);
        String getMultipleNotificationsText(Context context, int numberItems);

    }
    private ActivityManager activityManager;
    private Context context;

    public NotificationUtil(Context context) {
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.context = context;
    }

    private boolean isForegroundLollipop() {
        final int PROCESS_STATE_TOP = 2;
        Field field;
        try {
            field = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo app : appList) {
            if (app.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    app.importanceReasonCode == 0 ) {
                Integer state;
                try {
                    state = field.getInt( app );
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                if (state != null && state == PROCESS_STATE_TOP) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isForegroundCompat() {
        final List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        final String[] activePackages = new String[1];
        activePackages[0] = componentName.getPackageName();
        String packageName = this.context.getPackageName();
        if (activePackages != null) {
            for (String activePackage : activePackages) {
                if (activePackage.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isForeground() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses != null) {
            for(ActivityManager.RunningAppProcessInfo appProcess : appProcesses){
                if(appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                    return true;
                }
            }
        }
        return false;
    }

    public void showNotification(Intent resultIntent, int notificationId, int iconResId, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(text)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        resultIntent.setAction(Long.toString(System.currentTimeMillis()));
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.context);
        // Adds the back stack for the Intent (but not the Intent itself)
        ComponentName cmp = resultIntent.getComponent();

        Class destinationActivity;
        try {
            destinationActivity = Class.forName(cmp.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        stackBuilder.addParentStack(destinationActivity);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_ONE_SHOT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(notificationId, builder.build());
    }

    public static long getLastNotificationTime(Class klass, String modifier) {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        String key = klass.getSimpleName() + "." + LAST_NOTIFICATION_KEY;
        if (modifier != null) {
            key += "." + modifier;
        }
        return sharedPref.getLong(key, 1449716523208L);
    }

    public static void setLastNotificationTime(Class klass, String modifier, long time) {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();

        String key = klass.getSimpleName() + "." + LAST_NOTIFICATION_KEY;
        if (modifier != null) {
            key += "." + modifier;
        }

        editor.putLong(key, time);
        editor.commit();
    }

    public static void incrementShownNotificationCount(String identifier) {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        String mapString = sharedPref.getString(SHOWN_NOTIFICATION_COUNT, null);
        try {
            JSONObject jsonObject;
            if (mapString != null) {
                jsonObject = new JSONObject(mapString);
            } else {
                jsonObject = new JSONObject();
            }
            int currentCount = jsonObject.optInt(identifier, 0);
            currentCount = currentCount + 1;
            jsonObject.put(identifier, currentCount);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(SHOWN_NOTIFICATION_COUNT, jsonObject.toString());
            editor.commit();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getShownNotificationCount(String identifier) {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        String mapString = sharedPref.getString(SHOWN_NOTIFICATION_COUNT, null);
        try {
            JSONObject jsonObject;
            if (mapString != null) {
                jsonObject = new JSONObject(mapString);
            } else {
                jsonObject = new JSONObject();
            }
            return jsonObject.optInt(identifier, 0);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearShownNotificationCount() {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(SHOWN_NOTIFICATION_COUNT);
        editor.commit();
    }

    public static void clearNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        clearShownNotificationCount();
    }

    public static long getLastNotificationTime(Class klass) {
        return getLastNotificationTime(klass, null);
    }

    public static void setLastNotificationTime(Class klass, long time) {
        setLastNotificationTime(klass, null, time);
    }

    public static boolean defaultShouldShow(SyncModel item) {
        return item != null && item.isNew();
    }

    public static void setForeground(Boolean foreground) {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(FOREGROUND_KEY, foreground);
        editor.commit();
    }

    public static <T extends SyncModel> void addNotificationIfNeeded(Context context, String identifier, String groupedNotificationTitle, List<T> list, Bundle extras, Class activity, int drawableId, NotificationGenerator generator) {
        if (list == null) {
            return;
        }

        Collections.sort(list);
        List<Bundle> notificationsToShow = new ArrayList<>();

        NotificationUtil notificationUtil = new NotificationUtil(context);
        boolean foreground = notificationUtil.isForeground();

        // Checking if there are new objects
        int notificationId = identifier.hashCode();
        for (SyncModel item: list) {
            if (item == null) {
                continue;
            }
            Object[] result = generator.shouldDisplayNotification(context, item);
            Boolean shouldShow = (Boolean) result[0];
            String title = (String) result[1];
            String text = (String) result[2];
            Long id = (Long) result[3];

            if (shouldShow == null) {
                shouldShow = defaultShouldShow(item);
            }

            if (shouldShow) {
                Bundle notificationInfo = new Bundle();
                notificationInfo.putString("title", title);
                notificationInfo.putString("text", text);
                if (id != null) {
                    notificationInfo.putLong("id", id);
                }
                if (!foreground) {
                    notificationsToShow.add(notificationInfo);
                    incrementShownNotificationCount(identifier);
                }
            }

        }

        String title;
        String text;

        if (notificationsToShow.size() > 0) {

            int shownNotificationCount = getShownNotificationCount(identifier);
            String multipleNotificationText = generator.getMultipleNotificationsText(context, shownNotificationCount);
            if (shownNotificationCount == 1 || multipleNotificationText == null) {
                Bundle notificationInfo = notificationsToShow.get(0);
                Long id = notificationInfo.getLong("id", -1);
                if (id != -1) {
                    extras.putLong("detailItem", id);
                }
                title = notificationInfo.getString("title");
                text = notificationInfo.getString("text");
            } else if (shownNotificationCount > 1){
                title = groupedNotificationTitle;
                text = multipleNotificationText;
            } else {
                return;
            }


            Intent intent = new Intent(context, activity);
            intent.putExtras(extras);
            notificationUtil.showNotification(intent,
                    notificationId,
                    drawableId,
                    title,
                    text);
        }



    }
}
