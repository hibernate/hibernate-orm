/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
public class LobCreationCheckSkipTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, LobCreatorBuilderImpl.class.getName()
	) );

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000424:" );

	@Test
	public void test() {
		assertFalse(triggerable.wasTriggered());
	}
}
