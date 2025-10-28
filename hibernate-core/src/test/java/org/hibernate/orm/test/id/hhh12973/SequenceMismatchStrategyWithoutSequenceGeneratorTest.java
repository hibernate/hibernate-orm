/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.hhh12973;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.hibernate.id.enhanced.SequenceGeneratorLogger.SEQUENCE_GENERATOR_LOGGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12973")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceMismatchStrategyWithoutSequenceGeneratorTest extends EntityManagerFactoryBasedFunctionalTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( SEQUENCE_GENERATOR_LOGGER );

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH090203:" );

	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Override
	public EntityManagerFactory produceEntityManagerFactory() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( ApplicationConfigurationHBM2DDL.class )
				.buildMetadata();

		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
		return super.produceEntityManagerFactory();
	}

	@AfterAll
	public void releaseResources() {
		if ( metadata != null ) {
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
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
		triggerable.reset();
	}

	@Override
	protected void entityManagerFactoryBuilt(EntityManagerFactory factory) {
		assertFalse( triggerable.wasTriggered() );
	}

	@Test
	public void test() {

		final AtomicLong id = new AtomicLong();

		final int ITERATIONS = 51;

		inTransaction( entityManager -> {
			for ( int i = 1; i <= ITERATIONS; i++ ) {
				ApplicationConfiguration model = new ApplicationConfiguration();

				entityManager.persist( model );

				id.set( model.getId() );
			}
		} );

		assertEquals( ITERATIONS, id.get() );
	}

	@Entity(name = "ApplicationConfigurationHBM2DDL")
	@Table(name = "application_configurations")
	public static class ApplicationConfigurationHBM2DDL {

		@Id
		@jakarta.persistence.SequenceGenerator(
				name = "hibernate_sequence",
				sequenceName = "hibernate_sequence",
				allocationSize = 1
		)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}

	@Entity(name = "ApplicationConfiguration")
	@Table(name = "application_configurations")
	public static class ApplicationConfiguration {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}
}
