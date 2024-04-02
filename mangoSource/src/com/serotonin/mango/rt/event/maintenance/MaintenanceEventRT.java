package com.serotonin.mango.rt.event.maintenance;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.mango.Common;
import com.serotonin.mango.rt.event.EventInstance;
import com.serotonin.mango.rt.event.type.MaintenanceEventType;
import com.serotonin.mango.util.timeout.ModelTimeoutClient;
import com.serotonin.mango.util.timeout.ModelTimeoutTask;
import com.serotonin.mango.vo.event.MaintenanceEventVO;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.TimerTask;
import com.serotonin.timer.TimerTrigger;
import com.serotonin.web.i18n.LocalizableMessage;

public class MaintenanceEventRT implements ModelTimeoutClient<Boolean> {
    private final MaintenanceEventVO vo;
    private MaintenanceEventType eventType;
    private boolean eventActive;
    private TimerTask activeTask;
    private TimerTask inactiveTask;

    public MaintenanceEventRT(MaintenanceEventVO vo) {
        this.vo = vo;
    }

    public MaintenanceEventVO getVo() {
        return vo;
    }

    private void raiseEvent(long time) {
        if (!eventActive) {
            Common.ctx.getEventManager().raiseEvent(eventType, time, true, vo.getAlarmLevel(), getMessage(), null);
            eventActive = true;
        }
    }

    private void returnToNormal(long time) {
        if (eventActive) {
            Common.ctx.getEventManager().returnToNormal(eventType, time);
            eventActive = false;
        }
    }

    public LocalizableMessage getMessage() {
        return new LocalizableMessage("event.maintenance.active", vo.getDescription());
    }

    public boolean isEventActive() {
        return eventActive;
    }

    public boolean toggle() {
        scheduleTimeout(!eventActive, System.currentTimeMillis());
        return eventActive;
    }

    @Override
    synchronized public void scheduleTimeout(Boolean active, long fireTime) {
        if (active)
            raiseEvent(fireTime);
        else
            returnToNormal(fireTime);
    }

    //
    //
    // Lifecycle interface
    //
    public void initialize() {
        eventType = new MaintenanceEventType(vo.getId());

        if (vo.getScheduleType() != MaintenanceEventVO.TYPE_MANUAL) {
            // Schedule the active event.
            TimerTrigger activeTrigger = createTrigger(true);
            activeTask = new ModelTimeoutTask<Boolean>(activeTrigger, this, true);

            // Schedule the inactive event
            TimerTrigger inactiveTrigger = createTrigger(false);
            inactiveTask = new ModelTimeoutTask<Boolean>(inactiveTrigger, this, false);

            if (vo.getScheduleType() != MaintenanceEventVO.TYPE_ONCE) {
                // Check if we are currently active.
                if (inactiveTrigger.getNextExecutionTime() < activeTrigger.getNextExecutionTime())
                    raiseEvent(System.currentTimeMillis());
            }
        }
    }

    public void terminate() {
        if (activeTask != null)
            activeTask.cancel();
        if (inactiveTask != null)
            inactiveTask.cancel();

        if (eventActive)
            Common.ctx.getEventManager().returnToNormal(eventType, System.currentTimeMillis(),
                    EventInstance.RtnCauses.SOURCE_DISABLED);
    }

    public void joinTermination() {
        // no op
    }

    private static final String[] weekdays = { "", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };

    public TimerTrigger createTrigger(boolean activeTrigger) {
        return vo.createTrigger(activeTrigger);
    }
}
