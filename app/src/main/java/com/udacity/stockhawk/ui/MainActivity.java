package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;

    /**
     * These are from the ButterKnife library.
     * It is simply just a shorter version of setting our view objects to the appropriate ids
     */
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.error)
    TextView error;
    private StockAdapter adapter;
    private int lastSelectedStockPos = -1;


    boolean add = false;
    /**
     * simply tell us which stock we are pressing,
     * via a simple log library called Timber. This is just like our Logs, just made simple
     * Refactored this to send an intent to DetailActivity
     * @param symbol symbol of the stock list item being clicked
     */
    @Override
    public void onClick(String symbol, int position) {
        Timber.d("Symbol clicked: %s", symbol);

        lastSelectedStockPos = position;
        Intent detailIntent = new Intent(MainActivity.this,DetailActivity.class);
        detailIntent.putExtra(getString(R.string.clicked_symbol_key), symbol);
        startActivity(detailIntent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        if(lastSelectedStockPos != -1){
            outState.putInt(getString(R.string.last_stock_selected), lastSelectedStockPos);
        }
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //need this to bind all thew views that's annotated by @BindView
        ButterKnife.bind(this);

        adapter = new StockAdapter(this, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(this);
        //Notify the widget that refresh state has changed.
        //parameter:Whether or not the view should show refresh progress.
        swipeRefreshLayout.setRefreshing(true);

        QuoteSyncJob.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                PrefUtils.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
            }
        }).attachToRecyclerView(recyclerView);

        String lastStockSelectedKey = getString(R.string.last_stock_selected);
        if(savedInstanceState != null && savedInstanceState.containsKey(lastStockSelectedKey)){
            lastSelectedStockPos = savedInstanceState.getInt(lastStockSelectedKey);
        }

    }

    /**
     * Just a simple helper method to check network connection
     * This is used only in onRefresh()
     * @return true if connection is active, false otherwise
     */
    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }


    /**
     *This method is called when the listener detects a swipe refresh gesture
     *This method has been refactored by me (small refactor).
     *Starter code's order of calls in onCreate (onRefresh before QuoteSyncJob.initialise) has a flaw.
     * the case when there isn;t a connection but data has been loaded from previous use,
     * the toast message "Will refresh when network available" will not be met as the adapter will be empty.
     * since `syncImmediately` is run in background thread, it would not be able to load data in time to get to this case
     * So we will firstly move the error checks in this method to another method called checkErrors
     * Next we will remove onRefresh call from onCreate and checkError in onLoadFinished instead
     */
    @Override
    public void onRefresh() {

        QuoteSyncJob.syncImmediately(this);
        checkErrors();
    }

    private void  checkErrors(){
        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(this).size() == 0) {
            Timber.d("WHYAREWEHERE");
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }
    /**
     * This method is set in the "onClick" of our FloatingActionButton in activity_main.xml
     * This will just show our dialogActivity
     * @param view this is the FloatingActionButton
     */
    public void button(View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    /**
     * This method is a follow-up method from AddStockDialog.addStock
     * This method will just add the new stock via PrefUtils.addStock and then
     * we request to syncImmediately so that our list is updated
     * @param symbol the string given by the user from the AddStockDialog Activity's EditText
     */
    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {


            if (networkUp()) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.uri,
                Contract.Quote.QUOTE_COLUMNS,
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);

        String invalidStockSymbol = PrefUtils.getInvalidStock(this);
        if(invalidStockSymbol != null){
            Toast.makeText(this,getString(R.string.error_stock_not_found, invalidStockSymbol),Toast.LENGTH_SHORT).show();
        }

        checkErrors();

        if(lastSelectedStockPos != -1){
            recyclerView.smoothScrollToPosition(lastSelectedStockPos);
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }

    /**
     * Called in onCreateOptionMenu, helper method to display the correct unit symbol in menu
     * Also called in onOptionsItemSelected.
     * @param item
     */
    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    /**
     * Toggles between modes($ or %). once set, adapter is then updated in PrefUtils.toggleDisplayMode
     * and we notify that the data set has changed
     * @param item the item that is being selected
     * @return same as its super method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
