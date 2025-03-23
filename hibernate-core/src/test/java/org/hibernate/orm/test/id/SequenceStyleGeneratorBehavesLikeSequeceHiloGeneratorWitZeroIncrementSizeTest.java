/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsSequences;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
@RequiresDialectFeature(feature = SupportsSequences.class)
@BaseUnitTest
public class SequenceStyleGeneratorBehavesLikeSequeceHiloGeneratorWitZeroIncrementSizeTest {
	private static final String TEST_SEQUENCE = "test_sequence";

	private StandardServiceRegistry serviceRegistry;
	private SessionFactoryImplementor sessionFactory;
	private SequenceStyleGenerator generator;
	private SequenceValueExtractor sequenceValueExtractor;

	@BeforeEach
	public void setUp() throws Exception {
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.enableAutoClose()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();

		MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );

		generator = new SequenceStyleGenerator();

		// Build the properties used to configure the id generator
		Properties properties = new Properties();
		properties.setProperty( SequenceStyleGenerator.SEQUENCE_PARAM, TEST_SEQUENCE );
		properties.setProperty( SequenceStyleGenerator.OPT_PARAM, "legacy-hilo" );
		properties.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "0" ); // JPA allocationSize of 1
		generator.configure(
				new GeneratorCreationContext() {
					@Override
					public Database getDatabase() {
						return buildingContext.getMetadataCollector().getDatabase();
					}

					@Override
					public ServiceRegistry getServiceRegistry() {
						return serviceRegistry;
					}

					@Override
					public String getDefaultCatalog() {
						return "";
					}

					@Override
					public String getDefaultSchema() {
						return "";
					}

					@Override
					public PersistentClass getPersistentClass() {
						return null;
					}

					@Override
					public RootClass getRootClass() {
						return null;
					}

					@Override
					public Property getProperty() {
						return null;
					}

					@Override
					public Type getType() {
						return buildingContext.getBootstrapContext()
								.getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.LONG );
					}
				},
				properties
		);

		final Metadata metadata = new MetadataSources( serviceRegistry ).buildMetadata();
		generator.registerExportables( metadata.getDatabase() );

		sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		generator.initialize( sessionFactory.getSqlStringGenerationContext() );
		sequenceValueExtractor = new SequenceValueExtractor(sessionFactory.getJdbcServices().getDialect(), TEST_SEQUENCE );
	}

	@AfterEach
	public void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testHiLoAlgorithm() {
		TransactionUtil.doInHibernate(
				() -> sessionFactory,
				session -> {
					assertEquals( 1L, generateValue( session ) );

					assertEquals( 1L, extractSequenceValue( session ) );

					assertEquals( 2L, generateValue( session ) );
					assertEquals( 2L, extractSequenceValue( session ) );

					assertEquals( 3L, generateValue( session ) );
					assertEquals( 3L, extractSequenceValue( session ) );

					assertEquals( 4L, generateValue( session ) );
					assertEquals( 4L, extractSequenceValue( session ) );

					assertEquals( 5L, generateValue( session ) );
					assertEquals( 5L, extractSequenceValue( session ) );

				}
		);
	}

	private long extractSequenceValue(Session session) {
		return sequenceValueExtractor.extractSequenceValue( (SessionImplementor) session );
	}

	private long generateValue(Session session) {
		return (Long) generator.generate( (SharedSessionContractImplementor) session, null );
	}
}
