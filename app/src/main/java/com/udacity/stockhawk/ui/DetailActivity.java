package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;
import com.udacity.stockhawk.data.Contract;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class DetailActivity extends AppCompatActivity {

    @BindView(R.id.stock_detail_chart)
    LineChart stockDetailChart;
    @BindView(R.id.date_selected_textview)
    TextView selectedDateTextView;
    @BindView(R.id.price_selected_textview)
    TextView selectedPriceTextView;
    @BindView(R.id.price_highest_textview)
    TextView highestPriceTextView;
    @BindView(R.id.price_lowest_textview)
    TextView lowestPriceTextView;
    @BindView(R.id.price_highest_date_textview)
    TextView highestPriceDateTextView;
    @BindView(R.id.price_lowest_date_textview)
    TextView lowestPriceDateTextView;

    private static final int HISTORY_DATE_POSITION = 0;
    private static final int HISTORY_CLOSE_POSITION = 1;

    //going to make maxPriceStock for the graph to be 20% more than the highest stock price.
    private float maxPriceStock;
    private float highestPrice;
    private float lowestPrice;

    private int lowestPriceIndex;
    private int highestPriceIndex;
    private ArrayList<String> xAxisLabels;
    private ArrayList<String> datesForDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ButterKnife.bind(this);
       String symbol = getIntent().getStringExtra(getString(R.string.clicked_symbol_key));


        if(getSupportActionBar()!= null){
            getSupportActionBar().setTitle(symbol);
        }
        Cursor cursor = retrieveStockInfo(symbol);
        if(cursor != null) {
            cursor.moveToFirst();
            datesForDescription = new ArrayList<>();
            xAxisLabels = new ArrayList<>();

            String historyDataAsStr = cursor.getString(Contract.Quote.POSITION_HISTORY);
            String[] historyDataAsArr = historyDataAsStr.split("\n");

            int max = historyDataAsArr.length;

            List<Entry> entries = extractHistoryData(historyDataAsArr, max);
            LineData lineData = prepareChart(entries, symbol);

            stockDetailChart.setData(lineData);
            stockDetailChart.invalidate();


            stockDetailChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
               @Override
               public void onValueSelected(Entry e, Highlight h) {
                   Timber.d("value selected:%s, %f",xAxisLabels.get((int)e.getX()), e.getY());
                   selectedDateTextView.setText(datesForDescription.get((int)e.getX()));
                   selectedPriceTextView.setText(NumberFormat.getCurrencyInstance(Locale.getDefault()).format(e.getY()));
               }

               @Override
               public void onNothingSelected() {

               }

           });

            //last element is latest date
            selectedDateTextView.setText(datesForDescription.get(datesForDescription.size()-1));
            float currentPrice = entries.get(datesForDescription.size()-1).getY();
            selectedPriceTextView.setText(NumberFormat.getCurrencyInstance(Locale.getDefault()).format(currentPrice));
            highestPriceTextView.setText(NumberFormat.getCurrencyInstance(Locale.getDefault()).format(highestPrice));
            lowestPriceTextView.setText(NumberFormat.getCurrencyInstance(Locale.getDefault()).format(lowestPrice));
            lowestPriceDateTextView.setText(getString(R.string.lowest_highest_date_format,datesForDescription.get(lowestPriceIndex)));
            highestPriceDateTextView.setText(getString(R.string.lowest_highest_date_format,datesForDescription.get(highestPriceIndex)));


            cursor.close();
        }




    }

    private LineData prepareChart(List<Entry> entries, String symbol) {
        stockDetailChart.getXAxis().setValueFormatter(new MyXAxisValueFormatter(xAxisLabels));
        stockDetailChart.getXAxis().setTextColor(getResources().getColor(R.color.chart_value_text_color));
        stockDetailChart.getXAxis().setAxisMinimum(0);
        stockDetailChart.getXAxis().setAxisMaximum(xAxisLabels.size());
        stockDetailChart.getAxisLeft().setAxisMinimum(0);
        stockDetailChart.getAxisLeft().setAxisMaximum(maxPriceStock);
        stockDetailChart.getAxisLeft().setTextColor(getResources().getColor(R.color.chart_value_text_color));
        stockDetailChart.getAxisRight().setEnabled(false);

        String description = getString(R.string.chart_content_description, symbol);
        stockDetailChart.getDescription().setText(description);
        stockDetailChart.getDescription().setTextColor(getResources().getColor(R.color.chart_value_text_color));

        LineDataSet lineDataSet = new LineDataSet(entries,"");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        List<ILineDataSet> finalSet = new ArrayList<>();
        finalSet.add(lineDataSet);
        LineData lineData = new LineData(finalSet);
        lineData.setValueTextColor(getResources().getColor(R.color.chart_value_text_color));
        stockDetailChart.setBorderColor(getResources().getColor(R.color.chart_value_text_color));


        return lineData;
    }

    private Cursor retrieveStockInfo(String symbol){

        String selection = Contract.Quote.COLUMN_SYMBOL + "=?";
        String[] selectionArgs = {symbol};
        return getContentResolver().query(Contract.Quote.uri,Contract.Quote.QUOTE_COLUMNS,selection,selectionArgs,null);
    }

    private List<Entry> extractHistoryData(String[] historyDataArr, int max){
        ArrayList<Entry> entries = new ArrayList<>();

        boolean initialLoop = true;
        for(int index = 0; index < max; index++){
            String[] entryAsStr = historyDataArr[index].split(",");
            float close = Float.parseFloat(entryAsStr[HISTORY_CLOSE_POSITION]);
            //the entries taken are apparently backwards (latest to oldest), but we want oldest to latest, so we will go backwards
            Entry entry = new Entry(max - 1 - index, close);
            entries.add(0,entry);

            if(initialLoop) {
                highestPrice = close;
                lowestPrice = close;

                highestPriceIndex = index;
                lowestPriceIndex = index;

                initialLoop = false;
            }else{
                if(close > highestPrice){
                    highestPrice = close;
                    highestPriceIndex = max - 1 - index;
                }

                if(close < lowestPrice){
                    lowestPrice = close;
                    lowestPriceIndex = max - 1 - index;
                }
            }
            String chartDate = Utility.convertToChartDate(this,Long.parseLong(entryAsStr[HISTORY_DATE_POSITION]));
            xAxisLabels.add(0,chartDate);
            String descDate = Utility.convertToDescriptionDate(this,Long.parseLong(entryAsStr[HISTORY_DATE_POSITION]));
            datesForDescription.add(0, descDate);

        }

        maxPriceStock = highestPrice + (highestPrice * 0.20f);
        return entries;
    }

    public class MyXAxisValueFormatter implements IAxisValueFormatter{

        private ArrayList<String> dates;
        public MyXAxisValueFormatter(ArrayList<String> dates){
            this.dates = dates;
        }
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return dates.get((int)value);
        }

    }
}
