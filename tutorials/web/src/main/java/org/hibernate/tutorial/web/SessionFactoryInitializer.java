package org.hibernate.tutorial.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.hibernate.tutorial.util.HibernateUtil;

/**
 * Demonstrates good practice of making sure the SessionFactory is initialized
 * on application startup, rather than on first request.  Here we register
 * as a listener to the servlet context lifecycle for building/closing of the
 * SessionFactory.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryInitializer implements ServletContextListener {
	public void contextInitialized(ServletContextEvent event) {
		HibernateUtil.getSessionFactory();
	}

	public void contextDestroyed(ServletContextEvent event) {
		HibernateUtil.getSessionFactory().close();
	}
}
