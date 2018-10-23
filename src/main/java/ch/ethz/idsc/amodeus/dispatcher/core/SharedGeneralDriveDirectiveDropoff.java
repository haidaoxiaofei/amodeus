/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amodeus.dispatcher.core;

import java.util.Arrays;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;

import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.schedule.AVDriveTask;
import ch.ethz.matsim.av.schedule.AVDropoffTask;
import ch.ethz.matsim.av.schedule.AVStayTask;

/** for vehicles that are in stay task and should dropoff a customer at the link:
 * 1) finish stay task 2) append dropoff task 3) if more customers planned append drive task
 * 4) append new stay task */
/* package */ final class SharedGeneralDriveDirectiveDropoff extends FuturePathDirective {
    final RoboTaxi robotaxi;
    final AVRequest currentRequest;
    final double getTimeNow;
    final double dropoffDurationPerStop;

    public SharedGeneralDriveDirectiveDropoff(RoboTaxi robotaxi, AVRequest currentRequest, //
            FuturePathContainer futurePathContainer, final double getTimeNow, double dropoffDurationPerStop) {
        super(futurePathContainer);
        this.robotaxi = robotaxi;
        this.currentRequest = currentRequest;
        this.getTimeNow = getTimeNow;
        this.dropoffDurationPerStop = dropoffDurationPerStop;
    }

    // TODO mke sure its clear what it means if the VrpRath is null or the start and end link are equal. only one case is better
    @Override
    void executeWithPath(final VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = robotaxi.getSchedule();
        final AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = avStayTask.getEndTime();
        GlobalAssert.that(scheduleEndTime == schedule.getEndTime());
        final boolean moreRequestsToServe = (vrpPathWithTravelData != null);
        final double endTimeNextTask = (moreRequestsToServe) ? vrpPathWithTravelData.getArrivalTime() : getTimeNow + dropoffDurationPerStop;
        GlobalAssert.that(avStayTask.getLink().equals(currentRequest.getToLink()));

        if (endTimeNextTask < scheduleEndTime) {

            avStayTask.setEndTime(getTimeNow); // finish the last task now

            schedule.addTask(new AVDropoffTask( //
                    getTimeNow, // start of dropoff
                    getTimeNow + dropoffDurationPerStop, // end of dropoff
                    currentRequest.getToLink(), // location of dropoff
                    Arrays.asList(currentRequest)));

            // TODO can be done with less lines
            Link destLink = null;
            if (!vrpPathWithTravelData.getFromLink().equals(vrpPathWithTravelData.getToLink())) {
                schedule.addTask(new AVDriveTask( //
                        vrpPathWithTravelData));
                destLink = vrpPathWithTravelData.getToLink();
            } else {
                destLink = avStayTask.getLink();
            }
            ScheduleUtils.makeWhole(robotaxi, endTimeNextTask, scheduleEndTime, destLink);

            // jan: following computation is mandatory for the internal scoring
            // function
            // final double distance = VrpPathUtils.getDistance(vrpPathWithTravelData);
            // nextRequest.getRoute().setDistance(distance);
        } else
            reportExecutionBypass(endTimeNextTask - scheduleEndTime);
    }

}
