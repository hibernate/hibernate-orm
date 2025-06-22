/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionImpl;
import org.hibernate.testing.orm.junit.JiraKey;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.lang.invoke.MethodHandles;

/**
 * Example usage for the JUnit rule to assert logging events
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
@JiraKey(value = "HHH-9658")
public class LoggingRuleTest {

	//Taking this specific logger as a representative example of a Logger
	//(The purpose of this test is not to log but to exercise the logger methods)
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, SessionImpl.class.getName() );

	//We'll generally not be able to access the same LOG *instance* so make sure a fresh lookup
	//from Logger#getMessageLogger will work fine as well
	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, SessionImpl.class.getName() ) );

	@Test
	public void testRule() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000229:" );
		Assert.assertFalse( triggerable.wasTriggered() );
		LOG.runningSchemaValidator(); //Uses code HHH000229
		Assert.assertTrue( triggerable.wasTriggered() );
		triggerable.reset();
		Assert.assertFalse( triggerable.wasTriggered() );
	}

}
