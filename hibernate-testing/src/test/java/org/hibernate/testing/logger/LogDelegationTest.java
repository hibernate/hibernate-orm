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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2015 Red Hat Inc.
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
