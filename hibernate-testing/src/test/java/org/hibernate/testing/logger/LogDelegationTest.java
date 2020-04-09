/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import org.hibernate.testing.TestForIssue;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.junit.Test;

/**
 * Tests the TestHelper ..
 * Verifies the Logger interception capabilities which we might use in other tests
 * are working as expected.
 *
 * @author <a href="mailto:sanne@hibernate.org">Sanne Grinovero</a> (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HHH-9658")
public class LogDelegationTest {

	private static final Logger LOG = Logger.getLogger( LogDelegationTest.class.getName() );

	@Test
	public void testLogDelegationIsActivated() {
		assertThat( LOG, instanceOf( Log4DelegatingLogger.class ) );
	}

	@Test
	public void testRecording() {
		TestListener listener = new TestListener();
		LogInspectionHelper.registerListener( listener, LOG );

		LOG.debug( "Hey coffee is ready!" );
		assertThat( listener.isCAlled, is( true ) );
		assertThat( listener.level, is( Level.DEBUG ) );
		assertThat( (String) listener.renderedMessage, is( "Hey coffee is ready!" ) );
		assertThat( listener.thrown, nullValue() );
		LogInspectionHelper.clearAllListeners( LOG );
	}

	@Test
	public void testClearListeners() {
		TestListener listener = new TestListener();
		LogInspectionHelper.registerListener( listener, LOG );
		LogInspectionHelper.clearAllListeners( LOG );

		LOG.debug( "Hey coffee is ready!" );
		assertThat( listener.isCAlled, is( false ) );
	}

	private static class TestListener implements LogListener {

		boolean isCAlled = false;
		Level level;
		String renderedMessage;
		Throwable thrown;

		@Override
		public void loggedEvent(Level level, String renderedMessage, Throwable thrown) {
			this.level = level;
			this.renderedMessage = renderedMessage;
			this.thrown = thrown;
			this.isCAlled = true;
		}

	}

}
