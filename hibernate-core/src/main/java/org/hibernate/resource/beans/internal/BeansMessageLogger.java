/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10005001, max = 10010000 )
public interface BeansMessageLogger {
	/**
	 * *The* BeansMessageLogger instance
	 */
	BeansMessageLogger BEANS_LOGGER = Logger.getMessageLogger(
			BeansMessageLogger.class,
			"org.hibernate.orm.beans"
	);

	@LogMessage( level = WARN )
	@Message(
			id = 10005001,
			value = "An explicit CDI BeanManager reference [%s] was passed to Hibernate, " +
					"but CDI is not available on the Hibernate ClassLoader.  This is likely " +
					"going to lead to exceptions later on in bootstrap"
	)
	void beanManagerButCdiNotAvailable(Object cdiBeanManagerReference);

	@LogMessage( level = INFO )
	@Message(
			id = 10005002,
			value = "No explicit CDI BeanManager reference was passed to Hibernate, " +
					"but CDI is available on the Hibernate ClassLoader."
	)
	void noBeanManagerButCdiAvailable();

	@LogMessage( level = INFO )
	@Message(
			id = 10005003,
			value = "Stopping ManagedBeanRegistry : %s"
	)
	void stoppingManagedBeanRegistry(ManagedBeanRegistry registry);

	@LogMessage( level = INFO )
	@Message(
			id = 10005004,
			value = "Stopping BeanContainer : %s"
	)
	void stoppingBeanContainer(BeanContainer beanContainer);
}
