package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.ui.DetailActivity;
import com.udacity.stockhawk.ui.MainActivity;

/**
 * Created by mulya on 5/12/2016.
 */

public class StockDetailWidgetProvider extends AppWidgetProvider {

    private static final String GET_DETAIL = "android.com.udacity.stockhawk.GET_DETAIL";
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for(int appWidgetId : appWidgetIds){
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stock_detail_widget_layout);
            Intent intent = new Intent(context, MainActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                        views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            views.setRemoteAdapter(R.id.widget_listview,
                    new Intent(context, StockDetailWidgetRemoteViewsService.class));


            Intent clickIntentTemplate = new Intent(context, DetailActivity.class);

            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            clickIntentTemplate.setAction(GET_DETAIL);
            views.setPendingIntentTemplate(R.id.widget_listview, clickPendingIntentTemplate);
            //views.setEmptyView(R.id.widget_listview, R.id.widget_empty);


            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(GET_DETAIL)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_listview);
        }
        super.onReceive(context, intent);
    }

}
