/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.hhh12973;

import java.util.EnumSet;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12973")
@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
public class SequenceMismatchStrategyLogTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger(
					CoreMessageLogger.class,
					SequenceStyleGenerator.class.getName()
			)
	);

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000497:" );

	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Override
	public void buildEntityManagerFactory() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( ApplicationConfigurationHBM2DDL.class )
				.buildMetadata();

		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
		super.buildEntityManagerFactory();
	}

	@Override
	public void releaseResources() {
		super.releaseResources();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ApplicationConfiguration.class,
		};
	}

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.HBM2DDL_AUTO, "none" );
		settings.put( AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, "log" );
		triggerable.reset();
		assertFalse( triggerable.wasTriggered() );
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	public void test() {
	}

	@Entity
	@Table(name = "application_configurations")
	public static class ApplicationConfigurationHBM2DDL {

		@Id
		@javax.persistence.SequenceGenerator(
				name = "app_config_sequence",
				sequenceName = "app_config_sequence",
				allocationSize = 1
		)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_config_sequence")
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "application_configurations")
	public static class ApplicationConfiguration {

		@Id
		@javax.persistence.SequenceGenerator(
				name = "app_config_sequence",
				sequenceName = "app_config_sequence"
		)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_config_sequence")
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}
}
