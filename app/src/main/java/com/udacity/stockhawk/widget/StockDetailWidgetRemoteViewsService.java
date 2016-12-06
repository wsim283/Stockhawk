package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.ViewSwitcher;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by mulya on 5/12/2016.
 */

public class StockDetailWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory();
    }

    class ListRemoteViewsFactory implements RemoteViewsFactory {

        DecimalFormat percentageFormat;
        DecimalFormat dollarFormatWithPlus;
        private Cursor data = null;

        public ListRemoteViewsFactory(){
            dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+$");

            percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");
        }
        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
            if (data != null) {
                data.close();
            }

            final long identityToken = Binder.clearCallingIdentity();

            data = getContentResolver().query(Contract.Quote.uri,
                    Contract.Quote.QUOTE_COLUMNS,
                    null,
                    null,
                    Contract.Quote.COLUMN_SYMBOL);
            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public void onDestroy() {
            if (data != null) {
                data.close();
                data = null;
            }
        }

        @Override
        public int getCount() {
            return (data == null)? 0 : data.getCount();
        }

        @Override
        public RemoteViews getViewAt(int i) {

            if (i == AdapterView.INVALID_POSITION ||
                    data == null || !data.moveToPosition(i)) {
                return null;
            }

            RemoteViews views = new RemoteViews(getPackageName(),
                    R.layout.stock_detail_widget_list_item);


            String symbol = data.getString(Contract.Quote.POSITION_SYMBOL);
            views.setTextViewText(R.id.widget_symbol,symbol);
            views.setTextViewText(R.id.widget_price, NumberFormat.getCurrencyInstance(Locale.getDefault()).format(data.getFloat(Contract.Quote.POSITION_PRICE)));


            float rawAbsoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            if (rawAbsoluteChange > 0) {
               views.setInt(R.id.widget_change,"setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                views.setInt(R.id.widget_change,"setBackgroundResource", R.drawable.percent_change_pill_red);
            }

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            if (PrefUtils.getDisplayMode(getApplicationContext())
                    .equals(getString(R.string.pref_display_mode_absolute_key))) {
                views.setTextViewText(R.id.widget_change,change);
            } else {
                views.setTextViewText(R.id.widget_change,percentage);
            }


            Intent fillInIntent = new Intent();
            fillInIntent.putExtra(getString(R.string.clicked_symbol_key), symbol);
            views.setOnClickFillInIntent(R.id.widget_detail_item, fillInIntent);

            return views;

        }

        @Override
        public RemoteViews getLoadingView() {

            return new RemoteViews(getPackageName(), R.layout.stock_detail_widget_list_item);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int i) {
            if(data.moveToPosition(i)){
               return data.getLong(Contract.Quote.POSITION_ID);
            }
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
