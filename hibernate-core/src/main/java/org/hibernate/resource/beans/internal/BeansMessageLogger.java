/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.resource.beans.container.spi.BeanContainer;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10005001, max = 10010000 )
@SubSystemLogging(
		name = BeansMessageLogger.LOGGER_NAME,
		description = "Logging related to managed beans and the BeanContainer (CDI, etc)"
)
@Internal
public interface BeansMessageLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".beans";

	BeansMessageLogger BEANS_MSG_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), BeansMessageLogger.class, LOGGER_NAME );

	@LogMessage( level = INFO )
	@Message(
			id = 10005002,
			value = "No explicit CDI BeanManager reference was passed to Hibernate, " +
					"but CDI is available on the Hibernate class loader"
	)
	void noBeanManagerButCdiAvailable();

	@LogMessage( level = DEBUG )
	@Message(
			id = 10005004,
			value = "Stopping BeanContainer: %s"
	)
	void stoppingBeanContainer(BeanContainer beanContainer);

	@LogMessage( level = DEBUG )
	@Message(
			id = 10005006,
			value = "Standard access to BeanManager"
	)
	void standardAccessToBeanManager();

	@LogMessage( level = DEBUG )
	@Message(
			id = 10005007,
			value = "Extended access to BeanManager"
	)
	void extendedAccessToBeanManager();
}
