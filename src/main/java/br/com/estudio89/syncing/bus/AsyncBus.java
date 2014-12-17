package br.com.estudio89.syncing.bus;

import android.os.Handler;
import android.os.Looper;
import com.squareup.otto.Bus;

/**
 * Created by luccascorrea on 12/2/14.
 */
public class AsyncBus extends Bus {
    private final Handler mHandler = new Handler(Looper.getMainLooper());


    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AsyncBus.super.post(event);
                }
            });
        }
    }

}
