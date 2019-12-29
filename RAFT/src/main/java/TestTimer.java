import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Simple demo that uses java.util.Timer to schedule a task
 * to execute once 5 seconds have passed.
 */

public class TestTimer {
    public static Timer heartBeatTimer;
    public static Timer electionReqTimer;
    public  static TimerTask heartBeatTask;
    public static TimerTask electionReqTask;
    public  static int heartBeatPeriod = 5, electionThreshold= 10;

    public static class ElectionTimerTask extends TimerTask {

        @Override
        public void run() {
            System.out.println("Election Start Ping");

        }
    }

    public static class HeartBeatTimerTask extends TimerTask {

        @Override
        public void run() {
            resetElectionTimer();
            System.out.println("HeartBeat Ping");
        }
    }

    public  static void main (String args[]) {
        //TestTimer tim = new TestTimer();
        heartBeatTimer = new Timer();
        electionReqTimer = new Timer();
        heartBeatTask = new HeartBeatTimerTask();
        electionReqTask = new ElectionTimerTask();

        heartBeatTimer.schedule(heartBeatTask,0,5000);
        electionReqTimer.schedule (electionReqTask,10000);

    }

    public static void resetElectionTimer() {
        if (electionReqTask != null) {
            electionReqTask.cancel();
            electionReqTimer.purge();
        }
        electionReqTask = new ElectionTimerTask();
        electionReqTimer.schedule(electionReqTask,10000);

    }
    //Timer timer;
    /*

    public TestTimer(int seconds) {
        timer = new Timer();
        timer.schedule(new RemindTask(), seconds*1000);
    }

    class RemindTask extends TimerTask {
        public void run() {
            System.out.println("Time's up!");
            timer.cancel(); //Terminate the timer thread
        }
    }

    public static void main(String args[]) {
        new TestTimer(5);
        System.out.println("Task scheduled.");
    }
    */



}