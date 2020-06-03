package edu.duke.oit.idms.proconsul;

import java.util.Timer;
import java.util.TimerTask;

public class CheckOutScheduler {

	private static CheckOutScheduler cos = null;
	private static Object mutex = new Object();
	private static TimerTask timed = null;
	private static Timer timer = null;
	
	
	private CheckOutScheduler() {
	}
	
	public TimerTask getTimed() {
		return timed;
	}
	
	public Timer getTimer() {
		return timer;
	}
	
	public static CheckOutScheduler getInstance() {
		if (cos == null) {
			synchronized(mutex) {
				if (cos == null) {
					cos = new CheckOutScheduler();
					//timed = new CheckOutManager();
					timed = CheckOutManager.getInstance();
					Timer timer = new Timer(true);
					timer.scheduleAtFixedRate(timed, 1200, 60000);  // every 1 minutes, static
				}
			}
		}
		return cos;
	}
}
