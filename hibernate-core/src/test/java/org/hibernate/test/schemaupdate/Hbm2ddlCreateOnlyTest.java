package org.hibernate.test.schemaupdate;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.test.mapping.ColumnWithExplicitReferenceToPrimaryTableTest.AnEntity;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.jboss.logging.Logger;
import org.junit.Rule;
import org.junit.Test;

public class Hbm2ddlCreateOnlyTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			CoreMessageLogger.class, SessionFactoryOptionsBuilder.class.getName() ) );

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	@TestForIssue(jiraKey = "HHH-12955")
	public void testColumnAnnotationWithExplicitReferenceToPrimaryTable() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( AnEntity.class.getName() );
			}
		};


		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-only" );

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
