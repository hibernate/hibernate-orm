/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-12763" )
public class JtaPlatformLoggingTest extends BaseNonConfigCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, JtaPlatformInitiator.class.getName() ) );

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000490" );

	@Override
	protected void addSettings(Map<String,Object> settings) {
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
	}

	@Test
	public void test() {
		assertTrue( triggerable.triggerMessage().startsWith("HHH000490: Using JTA platform"));
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
