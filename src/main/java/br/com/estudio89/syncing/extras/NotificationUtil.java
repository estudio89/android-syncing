package br.com.estudio89.syncing.extras;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.models.SyncModel;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by luccascorrea on 5/15/15.
 *
 */
public class NotificationUtil {
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            try {
                return isForegroundCompat();
            } catch (Exception e) {
                return isForegroundLollipop();
            }
        } else {
            return isForegroundCompat();
        }
    }

    public void showNotification(Intent resultIntent, int notificationId, int iconResId, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);

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
                        PendingIntent.FLAG_UPDATE_CURRENT
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
    public static long getLastNotificationTime(Class klass) {
        return getLastNotificationTime(klass, null);
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

    public static void setLastNotificationTime(Class klass, long time) {
        setLastNotificationTime(klass, null, time);
    }

    public static boolean defaultShouldShow(SyncModel item) {
        return item != null && item.isNew();
    }

    public static void addNotificationIfNeeded(Context context, List<? extends SyncModel> list, Bundle extras, Class activity, int notificationId, int drawableId, NotificationGenerator generator) {
        if (list == null) {
            return;
        }

        // Checking if there are new objects
        for (SyncModel item: list) {
            Object[] result = generator.shouldDisplayNotification(context, item);
            Boolean shouldShow = (Boolean) result[0];
            String title = (String) result[1];
            String text = (String) result[2];
            Long id = (Long) result[3];

            if (shouldShow == null) {
                shouldShow = defaultShouldShow(item);
            }

            NotificationUtil notificationUtil = new NotificationUtil(context);
            if (!notificationUtil.isForeground() && shouldShow) {
                Intent intent = new Intent(context, activity);
                if (id != null) {
                    extras.putLong("detailItem", id);
                }
                intent.putExtras(extras);
                notificationUtil.showNotification(intent, notificationId, drawableId, title,text);
            }
        }
    }
}
