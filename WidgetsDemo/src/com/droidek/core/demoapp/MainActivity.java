/**
 * Kiev, Ukraine.
 * 
 * 01-SEP-2012
 */
package com.droidek.core.demoapp;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.widget.TextView;

import com.droidek.core.uicomponents.GanttChartView;
import com.droidek.core.uicomponents.GanttChartView.ICallback;
import com.droidek.core.uicomponents.GanttChartView.ITask;

public class MainActivity extends Activity {
  private GanttChartView ganttChartView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView taskName = (TextView) findViewById(R.id.taskName);
        final TextView taskStartDate = (TextView) findViewById(R.id.taskStartDate);
        final TextView taskDuration = (TextView) findViewById(R.id.taskDuration);

        ganttChartView = (GanttChartView) findViewById(R.id.ganttChart);
        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ganttChartView.setMetrics(metrics);
        ganttChartView.setSelected(true);
        ganttChartView.setCallback(new ICallback() {
          
          @Override
          public void onInitComplete(GanttChartView ganttChartView) {
            final Calendar c = Calendar.getInstance();
            c.clear();
            c.set(2012, Calendar.JUNE, 4);
            ganttChartView.setStartDate(c.getTime());
          }

          @Override
          public void onDataRequest(GanttChartView ganttView) {

            final Calendar c = Calendar.getInstance();
            c.clear();

            c.set(2012, Calendar.JUNE, 4, 14, 0);
            ganttView.addTodoTask(7721, "Task#1", c.getTime(), 1);

//            ganttView.addEditedTask(domain.getId(), domain.getItemName().value, domain.getStartDate(), domain.getDuration());

            c.clear();
            c.set(2012, Calendar.JUNE, 4, 17, 0);
            ganttView.addTodoTask(7722, "Task#3", c.getTime(), 2);

            c.clear();
            c.set(2012, Calendar.JUNE, 4, 19, 0);
            ganttView.addTodoTask(7723, "Task#4", c.getTime(), 3);

            c.clear();
            c.set(2012, Calendar.JUNE, 5, 10, 0);
            ganttView.addTodoTask(7724, "Task#5", c.getTime(), 2);

            c.clear();
            c.set(2012, Calendar.JUNE, 5, 16, 0);
            ganttView.addTodoTask(7725, "Task#6", c.getTime(), 1);

            c.clear();
            c.set(2012, Calendar.JUNE, 6, 0, 0);
            ganttView.addTodoTask(7726, "Task#7", c.getTime(), 28);

            c.clear();
            c.set(2012, Calendar.JUNE, 8, 0, 0);
            ganttView.addTodoTask(7727, "Task#8", c.getTime(), 1);
          }
          
          @Override
          public void onSelect(ITask selected) {
            taskName.setText(selected.getName());
            taskStartDate.setText(String.format("Start: %td %<tB, %<tY %<tI%<tp", selected.getStartDate()));
            taskDuration.setText(String.format("Duration: %dh", selected.getDuration()));
          }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
