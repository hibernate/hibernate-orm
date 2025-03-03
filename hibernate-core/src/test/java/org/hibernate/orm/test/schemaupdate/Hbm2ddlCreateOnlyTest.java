/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import static org.junit.Assert.assertFalse;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.orm.test.jpa.mapping.ColumnWithExplicitReferenceToPrimaryTableTest.AnEntity;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.jboss.logging.Logger;
import org.junit.Rule;
import org.junit.Test;

public class Hbm2ddlCreateOnlyTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			MethodHandles.lookup(), CoreMessageLogger.class, SessionFactoryOptionsBuilder.class.getName() ) );

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	@JiraKey(value = "HHH-12955")
	public void testColumnAnnotationWithExplicitReferenceToPrimaryTable() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( AnEntity.class.getName() );
			}
		};


		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-only" );
		ServiceRegistryUtil.applySettings( settings );

		EntityManagerFactory emf = null;
		try {
			Triggerable triggerable = logInspection.watchForLogMessages( "Unrecognized " + AvailableSettings.HBM2DDL_AUTO + " value" );

			emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
			emf.createEntityManager();

			assertFalse( triggerable.wasTriggered() );
		}
		finally {
			if ( emf != null ) {
				emf.close();
			}
		}
	}
}
