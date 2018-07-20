/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tool.schema;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.process.internal.ScanningCoordinator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.test.resource.transaction.jta.JtaPlatformStandardTestingImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12763" )
public class JtaPlatformLoggingTest extends BaseNonConfigCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, JtaPlatformInitiator.class.getName() ) );

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000490" );

	@Override
	protected void addSettings(Map settings) {
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
	}

	@Test
	public void test() {
		assertEquals(
				"HHH000490: Using JtaPlatform implementation: [org.hibernate.testing.jta.TestingJtaPlatformImpl]",
				triggerable.triggerMessage()
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			TestEntity.class
		};
	}

	@Entity( name = "TestEntity" )
	@Table( name = "TestEntity" )
	public static class TestEntity {
		@Id
		public Integer id;
		String name;
	}
}
