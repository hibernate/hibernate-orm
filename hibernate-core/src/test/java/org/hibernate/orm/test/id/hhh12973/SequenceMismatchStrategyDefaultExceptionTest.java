/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.hhh12973;

import java.util.EnumSet;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ExceptionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12973")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceMismatchStrategyDefaultExceptionTest extends EntityManagerFactoryBasedFunctionalTest {

	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Override
	public EntityManagerFactory produceEntityManagerFactory() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( ApplicationConfigurationHBM2DDL.class )
				.buildMetadata();

		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
		try {
			super.produceEntityManagerFactory();

			fail( "Should throw MappingException!" );
		}
		catch (Exception e) {
			Throwable rootCause = ExceptionUtil.rootCause( e );
			assertTrue( rootCause instanceof MappingException );
			assertTrue( rootCause.getMessage().contains( "in the entity mapping while the associated database sequence increment size is" ) );
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
		return null;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ApplicationConfiguration.class,
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.HBM2DDL_AUTO, "none" );
	}

	@Override
	protected boolean exportSchema() {
		return false;
	}

	@Test
	public void test() {
		produceEntityManagerFactory();
	}

	@Entity
	@Table(name = "application_configurations")
	public static class ApplicationConfigurationHBM2DDL {

		@Id
		@jakarta.persistence.SequenceGenerator(
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
		@jakarta.persistence.SequenceGenerator(
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
