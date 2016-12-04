package com.udacity.stockhawk;

import android.annotation.SuppressLint;
import android.content.Context;

import com.github.mikephil.charting.data.Entry;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by mulya on 2/12/2016.
 */

public class Utility {

    @SuppressLint("SwitchIntDef")
    public static String convertToChartDate(Context context, long time){
        Calendar givenDate = Calendar.getInstance();
        givenDate.setTimeInMillis(time);

        String chartDateStr = context.getString(R.string.chart_date);
        String month;
        int year = givenDate.get(Calendar.YEAR);

        switch(givenDate.get(Calendar.MONTH)){
            case Calendar.JANUARY:
                month = "Jan";
                break;
            case Calendar.FEBRUARY:
                month = "Feb";
                break;
            case Calendar.MARCH:
                month = "Mar";
                break;
            case Calendar.APRIL:
                month = "Apr";
                break;
            case Calendar.MAY:
                month = "May";
                break;
            case Calendar.JUNE:
                month = "Jun";
                break;
            case Calendar.JULY:
                month = "Jul";
                break;
            case Calendar.AUGUST:
                month = "Aug";
                break;
            case Calendar.SEPTEMBER:
                month = "Sep";
                break;
            case Calendar.OCTOBER:
                month = "Oct";
                break;
            case Calendar.NOVEMBER:
                month = "Nov";
                break;
            default:
                month = "Dec";
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumIntegerDigits(2);

        return String.format(chartDateStr, month,  numberFormat.format(year));
    }

    @SuppressLint("SwitchIntDef")
    public static String convertToDescriptionDate(Context context, long time){
        Calendar givenDate = Calendar.getInstance();
        givenDate.setTimeInMillis(time);

        String chartDateStr = context.getString(R.string.desciption_date);
        String month;
        int year = givenDate.get(Calendar.YEAR);

        switch(givenDate.get(Calendar.MONTH)){
            case Calendar.JANUARY:
                month = "January";
                break;
            case Calendar.FEBRUARY:
                month = "February";
                break;
            case Calendar.MARCH:
                month = "March";
                break;
            case Calendar.APRIL:
                month = "April";
                break;
            case Calendar.MAY:
                month = "May";
                break;
            case Calendar.JUNE:
                month = "June";
                break;
            case Calendar.JULY:
                month = "July";
                break;
            case Calendar.AUGUST:
                month = "August";
                break;
            case Calendar.SEPTEMBER:
                month = "September";
                break;
            case Calendar.OCTOBER:
                month = "October";
                break;
            case Calendar.NOVEMBER:
                month = "November";
                break;
            default:
                month = "December";
        }


        return String.format(chartDateStr, month,  year);
    }
}
