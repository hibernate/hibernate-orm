/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
