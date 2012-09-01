/**
 * Kiev, Ukraine.
 * 
 * 08-JUN-2012
 */
package com.droidek.core.uicomponents;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

import com.droidek.core.util.Ptr;

/**
 * @author Alexander Evseenko
 *
 */
public class GanttChartView extends View {
  private static final int DEFAULT_DURATION = 1;
  private static final int HOUR_IN_MILLIS = 60*60*1000;
  private static final int ROW_H = 48;
  private static final int RULER_H = ROW_H*2;

  private int tasksNum = 7;

  private DisplayMetrics metrics;
  private Rect size;

  private TimeFrame frame;
  private ICallback callback;

  public interface ICallback {
    void onInitComplete(GanttChartView ganttChartView);
    void onDataRequest(GanttChartView ganttChartView);
    void onSelect(ITask selected);
  }

  private class FrameHeader {
    public final int slotsNum;
    public final int slotInMillis;
    public final int slotW;
    public final int slotH;
    private final String rulerGroupFormat;
    private final String rulerFormat;
    private final String[] rulerGroupTexts;
    private final String[] rulerTexts;

    public FrameHeader(int slotsNum, int slotInHours, String rulerGroupFormat, String rulerFormat) {
      this.slotsNum = slotsNum;
      this.slotInMillis = HOUR_IN_MILLIS * slotInHours;

      this.slotW = (size.right - size.left)/slotsNum;
      this.slotH = ROW_H;

      this.rulerGroupFormat = rulerGroupFormat;
      this.rulerFormat = rulerFormat;
      rulerGroupTexts = new String[slotsNum];
      rulerTexts = new String[slotsNum];
    }

    public void changeToSlots(Calendar date, int toSlots) {
      date.add(Calendar.MILLISECOND, slotInMillis*toSlots);
    }

    public void initRulerTexts(Calendar startPeriod) {
      final Calendar c = (Calendar) startPeriod.clone();
      String lastGroupText = String.format(rulerGroupFormat, c);
      rulerGroupTexts[0] = lastGroupText;
      for (int i = 0; i < slotsNum; i++) {
        if (i > 0) {
          final String groupText = String.format(rulerGroupFormat, c);
          if (lastGroupText.equals(groupText)) {
            rulerGroupTexts[i] = "";
          } else {
            lastGroupText = groupText;
            rulerGroupTexts[i] = lastGroupText;
          }
        }
        rulerTexts[i] = String.format(rulerFormat, c);

        changeToSlots(c, 1);
      }
    }

    public void drawRuler(Canvas canvas) {
      final Paint paint = new Paint();
      paint.setColor(Color.GRAY);
      paint.setStrokeWidth(2);
      paint.setTextSize(16);

      canvas.drawLine(size.left, 0, size.right, 0, paint);
      canvas.drawLine(size.left, RULER_H-2, size.right, RULER_H-2, paint);

      for (int x = 0, i = 0; i < slotsNum; x += slotW, i++) {
        canvas.drawLine(x, RULER_H-14, x, RULER_H-2, paint);
        canvas.drawText(rulerGroupTexts[i], x+2, RULER_H-48, paint);
        canvas.drawText(rulerTexts[i], x+2, RULER_H-14, paint);
      }
    }

  }

  private class TimeFrame implements ListIterator<FrameHeader> {
    private final List<FrameHeader> slots = new ArrayList<FrameHeader>();
    private ListIterator<FrameHeader> slotsIterator;

    private FrameHeader header;

    protected final Calendar startPeriod;
    private List<ITask> taskList;
    private int topTask;
    private ITask selectedTask;
    private int editedTask;

    public TimeFrame() {
      startPeriod = Calendar.getInstance();
      startPeriod.clear();

      taskList = new ArrayList<ITask>();
      topTask = 0;
      selectedTask = null;
      editedTask = -1;

      addTimeSlot(5, 24, "%tB, %<tY", "%td %<tb");
      addTimeSlot(6, 4,  "%td %<tb", "%tI%<tp");
      addTimeSlot(8, 1,  "%td %<tb", "%tI%<tp");

      initSlotSize();
    }

    private void addTimeSlot(int slotsNum, int slotInHours, String rulerGroupFormat, String rulerFormat) {
      slots.add(new FrameHeader(slotsNum, slotInHours, rulerGroupFormat, rulerFormat));
    }

    public FrameHeader getHeader() {
      return header;
    }

    public Date getStartDate() {
      return startPeriod.getTime();
    }

    public void setStartPeriod(Date startPeriod) {
      if (startPeriod != null) {
        this.startPeriod.setTime(startPeriod);
        this.header.initRulerTexts(this.startPeriod);
      }
    }

    public Date getComplDate() {
      final Calendar c = (Calendar) startPeriod.clone();
      c.add(Calendar.MILLISECOND, header.slotInMillis*header.slotsNum);
      return c.getTime();
    }

    public void initSlotSize() {
      slotsIterator = slots.listIterator(0);
      next();
    }

    public List<ITask> getVisibleTasks() {
      return taskList.subList(topTask, Math.min(topTask+tasksNum, taskList.size()));
    }

    public void addTask(ITask task) {
      taskList.add(task);
    }

    /**
     * Causes drawing the task in selected style.
     * 
     * @param task will be drawing as selected.
     */
    public void setSelectedTask(ITask task) {
      if (isSelected() && task != null) {
        selectedTask = task;
        doSelected(task);
      }
    }

    /**
     * Tracks the task as currently reviewed/allowed to edit. It's not the same as being selected,
     * i.e. any task could be selected, but only this one could be edited and therefore affects linked views/activities.
     * 
     * @param taskId a task allowed to be edit.
     */
    public void setEditedTask(long taskId) {
      for (final ITask task : taskList) {
        if (task.getId() == taskId) {
          setEditedTask(task);
          break;
        }
      }
    }

    public void setEditedTask(ITask editedTask) {
      if (this.editedTask >= 0) {
        taskList.set(this.editedTask, new TodoTask(taskList.get(this.editedTask)));
      }

      for (int i = 0; i < taskList.size(); i++) {
        if (taskList.get(i) == editedTask) {
          taskList.set(i, new EditedTask(editedTask));
          this.editedTask = i;
          break;
        }
      }
    }

    public void changeStartPeriod(int toSlots) {
      // set up a new start date for the frame
      startPeriod.add(Calendar.MILLISECOND, header.slotInMillis*toSlots);
      header.initRulerTexts(startPeriod);
    }

    public void changeTopTask(int toRows) {
      topTask += toRows;
      if (topTask+tasksNum > taskList.size()) {
        topTask--;
      }

      if (topTask < 0) {
        topTask = 0;
      }
    }

    public int getStartSlot(Date start) {
      final long delta = start.getTime() - startPeriod.getTimeInMillis();

      return (int) (delta / header.slotInMillis);
    }

    public int getComplSlot(Date start, int durationInHours) {
      final int durationInMillis = (int) (durationInHours*HOUR_IN_MILLIS);
      final int slots = (int) (durationInMillis / header.slotInMillis);

      if (slots*header.slotInMillis == durationInMillis) {
        return getStartSlot(start) + slots-1; // since this slot is already included in the start slot
      } else {
        return getStartSlot(start) + slots;
      }
    }

    public void zoomIn(int x) {
      if (hasNext()) {
        final ITask clickedTask = getTaskByX(x);
        next();
        // align start period of the frame
        if (clickedTask != null) {
          setStartPeriod(clickedTask.getStartDate());
        }
      }
    }

    public void zoomOut(int x) {
      if (hasPrevious()) {
        final ITask clickedTask = getTaskByX(x);
        previous();
        // align start period of the frame
        if (clickedTask != null) {
          setStartPeriod(clickedTask.getStartDate());
        }
      }
    }

    public boolean handleDragEvent(int x, int y, Point delta) {
      // 2nd term allows to make scrolling faster in corners of the frame
      boolean horizontal = (Math.abs(delta.x) > header.slotW) ||
          (Math.abs(delta.x) > 10 && (x < header.slotW || x > header.slotW*(header.slotsNum-1)));
      if (horizontal) {
        final ITask touchedTask = getTaskByTouch(x, y);
        if (touchedTask != null) {
          // handle the event on the task level
          touchedTask.handleDragEvent(delta);
          doSelected(touchedTask);
        } else {
          // handle the event on the frame level
          changeStartPeriod((int) Math.signum(delta.x));
        }
      }

      boolean vertical = (Math.abs(delta.y) > header.slotH);
      if (vertical) {
        if (delta.y > 0) {
          changeTopTask(1);
        } else if (delta.y < 0) {
          changeTopTask(-1);
        }
      }

      return (horizontal || vertical);
    }

    public ITask getTaskByX(int x) {
      final int slot = xToSlot(x);
      // iterate tasks in the slot
      for (final ITask task : getVisibleTasks()) {
        if (slot >= task.getStartSlot() && slot <= task.getComplSlot()) {
          return task;
        }
      }

      return null;
    }

    public ITask getTaskByTouch(int x, int y) {
      final int slot = xToSlot(x);
      final int row = yToRow(y);
      // find the task in the slot
      if (row >= 0 && row < getVisibleTasks().size()) {
        final ITask task = getVisibleTasks().get(row);
        if (slot >= task.getStartSlot() && slot <= task.getComplSlot()) {
          return task;
        }
      }

      return null;
    }

    public void setSelectedTaskByTouch(int x, int y) {
      setSelectedTask(getTaskByTouch(x, y));
    }

    @Override
    public boolean hasNext() {
      return slotsIterator.hasNext();
    }

    @Override
    public FrameHeader next() {
      final FrameHeader slot = slotsIterator.next();
      setSlotSizeInMillis(slot);

      return slot;
    }

    @Override
    public boolean hasPrevious() {
      return slotsIterator.hasPrevious();
    }

    @Override
    public int nextIndex() {
      return slotsIterator.nextIndex();
    }

    @Override
    public FrameHeader previous() {
      final FrameHeader slot = slotsIterator.previous();
      setSlotSizeInMillis(slot);

      return slot;
    }

    @Override
    public int previousIndex() {
      return slotsIterator.previousIndex();
    }

    @Override
    public void remove() {
      // intentionally clear      
    }

    @Override
    public void add(FrameHeader slot) {
      // intentionally clear      
    }

    @Override
    public void set(FrameHeader slot) {
      // intentionally clear      
    }

    private void setSlotSizeInMillis(FrameHeader header) {
      this.header = header;

      if (getSlotSizeInHours() >= 24) {
        // align start period to day limit
        startPeriod.set(Calendar.HOUR_OF_DAY, 0);
        startPeriod.set(Calendar.MINUTE, 0);
      }

      this.header.initRulerTexts(startPeriod);
    }

    private int getSlotSizeInHours() {
      return (header.slotInMillis / HOUR_IN_MILLIS);
    }

    public void drawTimeSlots(Canvas canvas) {
      paint.setColor(Color.DKGRAY);

      for (int x = header.slotW, i = 0; i < header.slotsNum; x += header.slotW, i++) {
        canvas.drawLine(x, RULER_H, x, size.bottom, paint);
      }
    }

    public void drawTasks(Canvas canvas) {
      paint.setColor(Color.WHITE);

      int y = RULER_H;
      final Paint textPaint = new Paint();
      textPaint.setColor(Color.GRAY);
      textPaint.setStrokeWidth(2);
      textPaint.setTextSize(20);
      for (final ITask task : getVisibleTasks()) {
        task.draw(y, canvas, textPaint);
// TODO extract to ITask
        y += header.slotH;
      }
    }
  }

  public interface ITask {
    long getId();
    String getName();
    Date getStartDate();
    int getDuration();
    Date getComplDate();
    int getStartSlot();
    int getComplSlot();
    boolean isDependsOn();
    void handleDragEvent(Point delta);
    void draw(int y, Canvas canvas, Paint textPaint);
  }

  public class TodoTask implements ITask {
    protected final long id;
    protected final String name;
    protected final Date start;
    protected final int duration;

    public TodoTask(ITask task) {
      this(task.getId(), task.getName(), task.getStartDate(), task.getDuration());
    }

    public TodoTask(long id, String name, Date start, int duration) {
      this.id = id;
      this.name = name;
      this.start = start;
      this.duration = duration;
    }
    
    @Override
    public long getId() {
      return id;
    }
    
    public String getName() {
      return name;
    }
    
    @Override
    public Date getStartDate() {
      return start;
    }
    
    @Override
    public int getDuration() {
      return duration;
    }
    
    @Override
    public Date getComplDate() {
      final Calendar c = Calendar.getInstance();
      c.setTime(getStartDate());
      c.add(Calendar.HOUR, duration);
      
      return c.getTime();
    }
    
    @Override
    public int getStartSlot() {
      final int startSlot = frame.getStartSlot(getStartDate());
      if (startSlot < 0 && frame.getComplSlot(getStartDate(), duration) >= 0) {
        return 0;
      }
      
      return startSlot;
    }
    
    @Override
    public int getComplSlot() {
      final int finishSlot = frame.getComplSlot(getStartDate(), duration);
      if (frame.getStartSlot(getStartDate()) < frame.getHeader().slotsNum && finishSlot >= frame.getHeader().slotsNum) {
        return frame.getHeader().slotsNum-1;
      }
      
      return finishSlot;
    }
    
    @Override
    public boolean isDependsOn() {
      return false;
    }

    @Override
    public void handleDragEvent(Point delta) {
      // redirect back to the frame
      frame.changeStartPeriod((int) Math.signum(delta.x));
    }

    @Override
    public void draw(int y, Canvas canvas, Paint textPaint) {
      final Rect rect = new Rect(slotToX(getStartSlot())+1, y+1, slotToX(getComplSlot())+frame.header.slotW-1, y+frame.header.slotH-1);
      canvas.drawRect(rect, paint);
      canvas.drawText(name, rect.left, (rect.bottom+rect.top)/2+paint.getTextSize()/2, textPaint);
      if (this == frame.selectedTask) {
        canvas.drawRect(rect, selectedPaint);
      }
    }
  }

  public class EditedTask extends TodoTask {
    private Date changedStart;

    public EditedTask(ITask task) {
      this(task.getId(), task.getName(), task.getStartDate(), task.getDuration());
    }

    public EditedTask(long id, String name, Date start, int duration) {
      super(id, name, start, duration);

      changedStart = start;
    }

    @Override
    public Date getStartDate() {
      return changedStart;
    }

    @Override
    public void handleDragEvent(Point delta) {
      final int slot = (int) Math.signum(delta.x);

      final Calendar c = Calendar.getInstance();
      c.setTime(getStartDate());

      if (slot == +1 && getStartSlot() == 0) {
        // scroll in first position
        frame.changeStartPeriod(-slot);
      }
      if (slot == -1 && getStartSlot() == frame.header.slotsNum-1) {
        // scroll in last position
        frame.changeStartPeriod(-slot);
      }
      frame.header.changeToSlots(c, -slot);
      changedStart = c.getTime();
    }

    @Override
    public void draw(int y, Canvas canvas, Paint textPaint) {
      final Paint editedPaint = new Paint();
      editedPaint.setColor(Color.GREEN);
      final Rect rect = new Rect(slotToX(getStartSlot())+1, y+1, slotToX(getComplSlot())+frame.header.slotW-1, y+frame.header.slotH-1);
      canvas.drawRect(rect, editedPaint);
      canvas.drawText(name, rect.left, (rect.bottom+rect.top)/2+paint.getTextSize()/2, textPaint);
      if (this == frame.selectedTask) {
        canvas.drawRect(rect, selectedPaint);
      }
    }
  }

  
  private enum TouchMode {
    DRAG, ZOOM;
  };

  private static class MotionEventWrapper {
    private int action;
    private int x, y;

    public MotionEventWrapper() {
      update(-1, 0, 0);
    }

    public void update(int action, int x, int y) {
      this.action = action;
      this.x = x;
      this.y = y;
    }

    public void update(MotionEvent event) {
      update(event.getAction(), (int) event.getX(), (int) event.getY());
    }
  }
  
  private class TouchHandler {
    private MotionEvent event;
    private final MotionEventWrapper prev = new MotionEventWrapper();
    private float oldDist = 0;
    private final Point pitchCenter = new Point();

    public void handle(final MotionEvent event) {
      this.event = event;

      switch (event.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        // initialize it for make delta
        prev.update(event);
        break;
      }
      case MotionEvent.ACTION_POINTER_DOWN: {
        oldDist = spacing();
        calcPitchCenter();
        break;
      }
      case MotionEvent.ACTION_MOVE: {
        if (getMode() == TouchMode.DRAG) {
          // clean the zoom functionality
          calcPitchCenter();
          // delegate to the time frame
          if (frame.handleDragEvent(prev.x, prev.y, getDelta())) {
            prev.update(event);
          }
        } else if (getMode() == TouchMode.ZOOM) {
          // pitch moving
          final float newDist = spacing();
          if (Math.abs(oldDist - newDist) > frame.getHeader().slotW) {
            if (newDist < oldDist) {
              frame.zoomOut(pitchCenter.x);
            } else {
              frame.zoomIn(pitchCenter.x);
            }
            
            oldDist = newDist;
          }
        }
        break;
      }
      case MotionEvent.ACTION_UP: {
        if (prev.action == MotionEvent.ACTION_DOWN) {
//          frame.handleUpEvent(prev.x, prev.y, getDalta())
          final Point delta = getDelta();

          if (Math.abs(delta.x) < frame.getHeader().slotW && Math.abs(delta.y) < frame.getHeader().slotH) {
            frame.setSelectedTaskByTouch(prev.x, prev.y);
          }
        }
        break;
      }
      case MotionEvent.ACTION_CANCEL: {
        break;
      }
      default:
        break;
      }
    }

    public Point getPitchCenter() {
      return pitchCenter;
    }

    public TouchMode getMode() {
      if (event == null) {
        return TouchMode.DRAG;
      }

      return (event.getPointerCount() == 2 ? TouchMode.ZOOM : TouchMode.DRAG);
    }

    private Point getDelta() {
      final Point result = new Point();
      result.x = (int) (prev.x - event.getX()); result.y = (int) (prev.y - event.getY());
      return result;
    }

    private void calcPitchCenter() {
      if (event.getPointerCount() > 1) {
        // midpoint
        pitchCenter.x = (int) ((event.getX(0) + event.getX(1)) / 2);
        pitchCenter.y = (int) ((event.getY(0) + event.getY(1)) / 2);
      } else {
        pitchCenter.x = -1; pitchCenter.y = -1;
      }
    }

    private float spacing() {
      if (event.getPointerCount() > 1) {
        final float x = event.getX(0) - event.getX(1);
        final float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x*x + y*y);
      }

      return 0;
    }
  }

  private final TouchHandler touchHandler = new TouchHandler();
  private final Paint paint = new Paint();
  private final Paint selectedPaint = new Paint();


  public GanttChartView(Context context, AttributeSet attrs) {
    super(context, attrs);

    selectedPaint.setColor(Color.GREEN);
    selectedPaint.setStyle(Style.STROKE);
    selectedPaint.setStrokeWidth(2);
  }

  public void setCallback(ICallback callback) {
    this.callback = callback;
  }

  private void doInitComplete() {
    if (callback != null) {
      callback.onInitComplete(this);
    }
  }

  private boolean doDataRequest() {
    frame.taskList.clear();

    if (callback != null) {
      callback.onDataRequest(this);
      return true;
    } else {
      return false;
    }
  }

  private void doSelected(ITask task) {
    if (callback != null && task != null) {
      callback.onSelect(task);
    }
  }

  public void addTodoTask(long id, String name, Date startDate, int duration) {
    frame.addTask(new TodoTask(id, name, startDate, duration));
  }

  public void addEditedTask(long id, String name, Ptr<Date> startDate, Ptr<Integer> duration) {
    if (id >= 0 && startDate.isDefined() && duration.isDefined()) {
      frame.addTask(new EditedTask(id, name, startDate.value, duration.value));
    } else {
      throw new IllegalArgumentException("The task must exist.");
    }
  }

  public void setEditedTask(ITask task) {
    frame.setEditedTask(task);

    invalidate();
  }

  public void setMetrics(DisplayMetrics metrics) {
    this.metrics = metrics;
  }

  public void setStartDate(Date startDate) {
    frame.setStartPeriod(startDate);
    doDataRequest();
  }

  public void newTask() {
    final TodoTask newTask = new TodoTask(-1, "New", new Date(), 1);
    frame.addTask(newTask);
    frame.setStartPeriod(newTask.getStartDate());
    frame.setSelectedTask(newTask);
  }

  public void zoomIn() {
    frame.zoomIn(size.left);
    invalidate();
  }

  public void zoomOut() {
    frame.zoomOut(size.left);
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
// FIXME
    setMeasuredDimension(480/*metrics.widthPixels*/, tasksNum*ROW_H+RULER_H);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    size = new Rect(left, top, right, bottom);
    // the time frame component depends on physical size
    frame = new TimeFrame();
    doInitComplete();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    frame.header.drawRuler(canvas);
    frame.drawTimeSlots(canvas);
    frame.drawTasks(canvas);
    drawPitchCircle(canvas);
  }

  private void drawPitchCircle(Canvas canvas) {
    if (touchHandler.getMode() == TouchMode.ZOOM) {
      final Paint paint = new Paint();
      paint.setStyle(Style.STROKE);
      paint.setColor(Color.DKGRAY);
      canvas.drawCircle(touchHandler.getPitchCenter().x, touchHandler.getPitchCenter().y, touchHandler.spacing()/2, paint);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    touchHandler.handle(e);
    invalidate();

    return true; // must be consumed to work in a pager fragment context
  }

  private int xToSlot(int x) {
    return x/frame.getHeader().slotW;
  }

  private int yToRow(int y) {
    return (y-RULER_H)/frame.getHeader().slotH;
  }

  private int slotToX(int slot) {
    return size.left + slot*frame.getHeader().slotW;
  }

}
