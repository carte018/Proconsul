package edu.duke.oit.idms.proconsul;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServletMaintainer implements ServletContextListener {
	CheckOutScheduler cos = null;
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		CheckOutScheduler.getInstance().getTimed().cancel();
		CheckOutScheduler.getInstance().getTimer().purge();
		CheckOutScheduler.getInstance().getTimer().cancel();
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// Instantiate the timed process
		cos = CheckOutScheduler.getInstance();
	}
}
