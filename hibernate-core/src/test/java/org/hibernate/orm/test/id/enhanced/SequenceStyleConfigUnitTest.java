/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.enhanced;

import java.util.Properties;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sequence.ANSISequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.hibernate.id.enhanced.PooledLoThreadLocalOptimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that SequenceStyleGenerator configures itself as expected in various scenarios
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
@RequiresDialect(
		value = H2Dialect.class,
		comment = "Even though we use specialized Dialects, we still have calls happening to the "
				+ "underlying driver which will blow up on various underlying drivers.  Nothing here is "
				+ "Dialect-specific anyway, besides what each specialized Dialect exposes."
)
public class SequenceStyleConfigUnitTest {
	private static final Type LONG_TYPE = new TypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.LONG );

	/**
	 * Test all params defaulted with a dialect supporting pooled sequences
	 */
	@Test
	public void testDefaultedSequenceBackedConfiguration() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build()) {
			MetadataBuildingContextTestingImpl buildingContext1 = new MetadataBuildingContextTestingImpl(
					serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext1.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			MetadataBuildingContext buildingContext = buildingContext1;
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure(
					new TestGeneratorCreationContext( LONG_TYPE, buildingContext, serviceRegistry ),
					props
			);

			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );

			final DatabaseStructure databaseStructure = generator.getDatabaseStructure();
			assertTrue( databaseStructure.isPhysicalSequence() );

			final Optimizer optimizer = generator.getOptimizer();
			assertThat( optimizer, instanceOf( PooledOptimizer.class ) );
			assertEquals( optimizer.getIncrementSize(), OptimizableGenerator.DEFAULT_INCREMENT_SIZE );

			assertEquals( "ID_SEQ", databaseStructure.getPhysicalName().render() );
		}
	}

	private Properties buildGeneratorPropertiesBase(MetadataBuildingContext buildingContext) {
		Properties props = new Properties();
		props.put(
				PersistentIdentifierGenerator.IMPLICIT_NAME_BASE,
				"ID"
		);
		return props;
	}

	/**
	 * Test all params defaulted with a dialect which does not support sequences
	 */
	@Test
	public void testDefaultedTableBackedConfiguration() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, TableDialect.class.getName() )
				.build()) {
			MetadataBuildingContextTestingImpl buildingContext1 = new MetadataBuildingContextTestingImpl(
					serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext1.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			MetadataBuildingContext buildingContext = buildingContext1;
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure(
					new TestGeneratorCreationContext( LONG_TYPE, buildingContext, serviceRegistry ),
					props
			);

			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );

			final DatabaseStructure databaseStructure = generator.getDatabaseStructure();
			final Optimizer optimizer = generator.getOptimizer();

			assertFalse( databaseStructure.isPhysicalSequence() );
			assertThat( optimizer, instanceOf( PooledOptimizer.class ) );
			assertEquals( 50, databaseStructure.getIncrementSize());

			assertEquals( "ID_SEQ", databaseStructure.getPhysicalName().render() );
		}
	}

	/**
	 * Test default optimizer selection for sequence backed generators
	 * based on the configured increment size; both in the case of the
	 * dialect supporting pooled sequences (pooled) and not (hilo)
	 */
	@Test
	public void testDefaultOptimizerBasedOnIncrementBackedBySequence() {
		// for dialects which do not support pooled sequences, we default to pooled+table
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build()) {
			MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure(
					new TestGeneratorCreationContext( LONG_TYPE, buildingContext, serviceRegistry ),
					props
			);

			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );

			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( "ID_SEQ",
					generator.getDatabaseStructure().getPhysicalName().render() );
		}

		// for dialects which do support pooled sequences, we default to pooled+sequence
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build()) {
			MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure(
					new TestGeneratorCreationContext( LONG_TYPE, buildingContext, serviceRegistry ),
					props
			);
			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );

			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( "ID_SEQ",
					generator.getDatabaseStructure().getPhysicalName().render() );
		}
	}

	/**
	 * Test default optimizer selection for table backed generators
	 * based on the configured increment size.  Here we always prefer
	 * pooled.
	 */
	@Test
	public void testDefaultOptimizerBasedOnIncrementBackedByTable() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, TableDialect.class.getName() )
				.build()) {
			MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure(
					new TestGeneratorCreationContext( LONG_TYPE, buildingContext, serviceRegistry ),
					props
			);
			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );

			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( "ID_SEQ",
					generator.getDatabaseStructure().getPhysicalName().render() );
		}
	}

	/**
	 * Test forcing of table as backing structure with dialect supporting sequences
	 */
	@Test
	public void testForceTableUse() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build()) {
			MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.FORCE_TBL_PARAM, "true" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure(
					new TestGeneratorCreationContext( LONG_TYPE, buildingContext, serviceRegistry ),
					props
			);
			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );

			final DatabaseStructure databaseStructure = generator.getDatabaseStructure();
			final Optimizer optimizer = generator.getOptimizer();

			assertFalse( databaseStructure.isPhysicalSequence() );
			assertThat( optimizer, instanceOf( PooledOptimizer.class ) );
			assertEquals( 50, databaseStructure.getIncrementSize());

			assertEquals( "ID_SEQ", databaseStructure.getPhysicalName().render() );
		}
	}

	/**
	 * Test explicitly specifying both optimizer and increment
	 */
	@Test
	public void testExplicitOptimizerWithExplicitIncrementSize() {
		// optimizer=none w/ increment > 1 => should honor optimizer
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build()) {
			MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			final GeneratorCreationContext creationContext = new TestGeneratorCreationContext(
					LONG_TYPE,
					buildingContext,
					serviceRegistry
			);
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.NONE.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( creationContext, props );
			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );

			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( NoopOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 1, generator.getOptimizer().getIncrementSize() );
			assertEquals( 1, generator.getDatabaseStructure().getIncrementSize() );

			// optimizer=hilo w/ increment > 1 => hilo
			props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.HILO.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			generator = new SequenceStyleGenerator();
			generator.configure( creationContext, props );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( HiLoOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 20, generator.getOptimizer().getIncrementSize() );
			assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );

			// optimizer=pooled w/ increment > 1 => hilo
			props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.POOLED.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			generator = new SequenceStyleGenerator();
			generator.configure( creationContext, props );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			// because the dialect reports to not support pooled seqyences, the expectation is that we will
			// use a table for the backing structure...
			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 20, generator.getOptimizer().getIncrementSize() );
			assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );
		}
	}

	@Test
	public void testPreferredPooledOptimizerSetting() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build()) {
			MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			serviceRegistry.getService( JdbcServices.class ).getDialect().contributeTypes(
					() -> buildingContext.getBootstrapContext().getTypeConfiguration(),
					serviceRegistry
			);
			final GeneratorCreationContext creationContext = new TestGeneratorCreationContext(
					LONG_TYPE,
					buildingContext,
					serviceRegistry
			);
			Properties props = buildGeneratorPropertiesBase( buildingContext );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( creationContext, props );
			Database database = new Database( buildingContext.getBuildingOptions() );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );

			props.setProperty( Environment.PREFERRED_POOLED_OPTIMIZER, StandardOptimizerDescriptor.POOLED_LO.getExternalName() );
			generator = new SequenceStyleGenerator();
			generator.configure( creationContext, props );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledLoOptimizer.class, generator.getOptimizer().getClass() );

			props.setProperty( Environment.PREFERRED_POOLED_OPTIMIZER, StandardOptimizerDescriptor.POOLED_LOTL.getExternalName() );
			generator = new SequenceStyleGenerator();
			generator.configure( creationContext, props );
			generator.registerExportables( database );
			generator.initialize( SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledLoThreadLocalOptimizer.class, generator.getOptimizer().getClass() );
		}
	}


	public static final SettingProvider.Provider<Class<? extends Dialect>> TABLE_DIALECT_PROVIDER = new SettingProvider.Provider<Class<? extends Dialect>>() {
		@Override
		public Class<? extends Dialect> getSetting() {
			return TableDialect.class;
		}
	};

	public static final SettingProvider.Provider<Class<? extends Dialect>> SEQUENCE_DIALECT_PROVIDER
			= () -> SequenceDialect.class;
	public static final SettingProvider.Provider<Class<? extends Dialect>> POOLED_SEQUENCE_DIALECT_PROVIDER
			= () -> PooledSequenceDialect.class;

	public static class TableDialect extends Dialect implements SettingProvider.Provider {
		@Override
		public DatabaseVersion getVersion() {
			return ZERO_VERSION;
		}

		@Override
		public Object getSetting() {
			return getClass();
		}
	}

	public static class SequenceDialect extends Dialect {
		@Override
		public DatabaseVersion getVersion() {
			return ZERO_VERSION;
		}

		@Override
		public SequenceSupport getSequenceSupport() {
			return new ANSISequenceSupport() {
				@Override
				public boolean supportsPooledSequences() {
					return false;
				}
			};
		}
	}

	public static class PooledSequenceDialect extends SequenceDialect {
		@Override
		public SequenceSupport getSequenceSupport() {
			return ANSISequenceSupport.INSTANCE;
		}
	}

	private static class TestGeneratorCreationContext implements GeneratorCreationContext {
		private final Type type;
		private final MetadataImplementor metadata;
		private final ServiceRegistry serviceRegistry;

		public TestGeneratorCreationContext(Type type, MetadataBuildingContext buildingContext, ServiceRegistry serviceRegistry) {
			this.type = type;
			this.metadata = buildingContext.getMetadataCollector();
			this.serviceRegistry = serviceRegistry;
		}

		@Override
		public Database getDatabase() {
			return metadata.getDatabase();
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
			return type;
		}
	}
}
