/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionImpl;
import org.hibernate.testing.TestForIssue;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Example usage for the JUnit rule to assert logging events
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HHH-9658")
public class LoggingRuleTest {

	//Taking this specific logger as a representative example of a Logger
	//(The purpose of this test is not to log but to exercise the logger methods)
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, SessionImpl.class.getName() );

	//We'll generally not be able to access the same LOG *instance* so make sure a fresh lookup
	//from Logger#getMessageLogger will work fine as well
	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger( CoreMessageLogger.class, SessionImpl.class.getName() ) );

	@Test
	public void testRule() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000008:" );
		Assert.assertFalse( triggerable.wasTriggered() );
		LOG.autoFlushWillNotWork(); //Uses code HHH000008
		Assert.assertTrue( triggerable.wasTriggered() );
		triggerable.reset();
		Assert.assertFalse( triggerable.wasTriggered() );
	}

}
