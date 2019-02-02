package org.hibernate.orm.test.jpa.tool.schemacreation;

import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionExtension;
import org.hibernate.testing.logger.Triggerable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class Hbm2ddlCreateOnlyTest extends EntityManagerFactoryBasedFunctionalTest {

	private Triggerable triggerable;

	public LoggerInspectionExtension logInspection = new LoggerInspectionExtension(
			Logger.getMessageLogger(
					CoreMessageLogger.class,
					SessionFactoryOptionsBuilder.class.getName()
			) );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AnEntity.class };
	}


	@Override
	protected void applySettings(Map<Object, Object> settings) {
		super.applySettings( settings );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-only" );
	}

	@Override
	protected boolean exportSchema() {
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	@TestForIssue(jiraKey = "HHH-12955")
	public void testWarnMessageIsNotTriggeredWhitCreateOnlySetting() {
		EntityManagerFactory emf = entityManagerFactory();
		try {

			emf.createEntityManager();

			assertFalse( triggerable.wasTriggered() );
		}
		finally {
			if ( emf != null ) {
				emf.close();
			}
		}
	}

	@BeforeAll
	public void beforeEach() {
		triggerable = logInspection.watchForLogMessages( "Unrecognized " + AvailableSettings.HBM2DDL_AUTO + " value" );
	}

	@AfterEach
	public void afterAll() {
		logInspection.afterEach();
	}

	@Entity
	@Table(name = "THE_TABLE")
	public static class AnEntity {
		@Id
		public Integer id;
		@Column(name = "THE_COLUMN", table = "THE_TABLE")
		public String theValue;
	}
}
