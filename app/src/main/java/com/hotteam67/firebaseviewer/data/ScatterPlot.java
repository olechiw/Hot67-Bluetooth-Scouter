package com.hotteam67.firebaseviewer.data;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.hotteam67.firebaseviewer.MainActivity;
import com.hotteam67.firebaseviewer.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jakob on 4/6/2018.
 */

public class ScatterPlot {
    public static void Show(List<Integer> inputValues, Context c, String title)
    {
        // Populate y-axis
        List<Integer> yValues = new ArrayList<>();
        yValues.addAll(inputValues);

        // Populate x-axis
        List<Integer> xValues = new ArrayList<>();
        for (int i = 0; i < yValues.size(); ++i)
            xValues.add(i + 1);

        // Interleave
        List<Integer> xyValues = new ArrayList<>();
        for (int i = 0; i < yValues.size(); ++i)
        {
            xyValues.add(xValues.get(i));
            xyValues.add(yValues.get(i));
        }

        XYSeries series = new SimpleXYSeries(
                xyValues, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "");

        PixelUtils.init(c);

        LineAndPointFormatter format =
                new LineAndPointFormatter(Color.RED, Color.BLACK, null, null);

        XYPlot newPlot = (XYPlot)((MainActivity)c).getLayoutInflater().inflate(R.layout.xyplot, null);
        newPlot.setTitle(title);

        newPlot.setDomainBoundaries(0, BoundaryMode.FIXED, xValues.size() + 1, BoundaryMode.FIXED);
        newPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 1);

        newPlot.getLayoutManager().remove(newPlot.getLegend());

        // Find max for range
        Integer max = 0;
        for (Integer i : yValues)
            if (i > max)
                max = i;
        newPlot.setRangeBoundaries(0, BoundaryMode.FIXED, max + 1, BoundaryMode.FIXED);
        newPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 1);
        newPlot.setLinesPerDomainLabel(1);
        newPlot.setLinesPerRangeLabel(1);

        newPlot.getGraph().setMargins(100, 100, 35, 50);
        newPlot.setBackgroundColor(Color.WHITE);
        newPlot.getGraph().getBackgroundPaint().setColor(Color.WHITE);
        newPlot.getGraph().getGridBackgroundPaint().setColor(Color.WHITE);
        newPlot.getBackgroundPaint().setColor(Color.WHITE);

        newPlot.getGraph().getDomainGridLinePaint().setColor(Color.BLACK);
        newPlot.getGraph().getRangeGridLinePaint().setColor(Color.BLACK);

        newPlot.getGraph().getDomainSubGridLinePaint().setColor(Color.BLACK);
        newPlot.getGraph().getRangeSubGridLinePaint().setColor(Color.BLACK);
        newPlot.getGraph().getDomainOriginLinePaint().setColor(Color.BLACK);
        newPlot.getGraph().getRangeOriginLinePaint().setColor(Color.BLACK);

        newPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new DecimalFormat("#"));

        newPlot.getDomainTitle().getLabelPaint().setColor(Color.BLACK);
        newPlot.getRangeTitle().getLabelPaint().setColor(Color.BLACK);
        newPlot.getTitle().getLabelPaint().setColor(Color.BLACK);


        newPlot.addSeries(format, series);
        newPlot.redraw();

        AlertDialog.Builder builder = new AlertDialog.Builder(
                c, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        final AlertDialog dialog = builder.setView(newPlot).show();
        newPlot.setOnClickListener(click -> dialog.dismiss());
    }
}