/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.enhanced;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
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
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
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
public class SequenceStyleConfigUnitTest {

	/**
	 * Test all params defaulted with a dialect supporting pooled sequences
	 */
	@Test
	public void testDefaultedSequenceBackedConfiguration() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );

			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);

			final DatabaseStructure databaseStructure = generator.getDatabaseStructure();
			assertTrue( databaseStructure.isPhysicalSequence() );

			final Optimizer optimizer = generator.getOptimizer();
			assertThat( optimizer, instanceOf( PooledOptimizer.class ) );
			assertEquals( optimizer.getIncrementSize(), OptimizableGenerator.DEFAULT_INCREMENT_SIZE );

			assertEquals( "ID_SEQ", databaseStructure.getName() );
		}
	}

	private Properties buildGeneratorPropertiesBase(StandardServiceRegistry serviceRegistry) {
		Properties props = new Properties();
		props.put(
				PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
				new MetadataBuildingContextTestingImpl( serviceRegistry ).getObjectNameNormalizer()
		);
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
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, TableDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );

			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);

			final DatabaseStructure databaseStructure = generator.getDatabaseStructure();
			final Optimizer optimizer = generator.getOptimizer();

			assertFalse( databaseStructure.isPhysicalSequence() );
			assertThat( optimizer, instanceOf( PooledOptimizer.class ) );
			assertEquals( 50, databaseStructure.getIncrementSize());

			assertEquals( "ID_SEQ", databaseStructure.getName() );
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
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );

			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);

			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( "ID_SEQ", generator.getDatabaseStructure().getName() );
		}

		// for dialects which do support pooled sequences, we default to pooled+sequence
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);

			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( "ID_SEQ", generator.getDatabaseStructure().getName() );
		}
	}

	/**
	 * Test default optimizer selection for table backed generators
	 * based on the configured increment size.  Here we always prefer
	 * pooled.
	 */
	@Test
	public void testDefaultOptimizerBasedOnIncrementBackedByTable() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, TableDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);

			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( "ID_SEQ", generator.getDatabaseStructure().getName() );
		}
	}

	/**
	 * Test forcing of table as backing structure with dialect supporting sequences
	 */
	@Test
	public void testForceTableUse() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.FORCE_TBL_PARAM, "true" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);

			final DatabaseStructure databaseStructure = generator.getDatabaseStructure();
			final Optimizer optimizer = generator.getOptimizer();

			assertFalse( databaseStructure.isPhysicalSequence() );
			assertThat( optimizer, instanceOf( PooledOptimizer.class ) );
			assertEquals( 50, databaseStructure.getIncrementSize());

			assertEquals( "ID_SEQ", databaseStructure.getName() );
		}
	}

	/**
	 * Test explicitly specifying both optimizer and increment
	 */
	@Test
	public void testExplicitOptimizerWithExplicitIncrementSize() {
		// optimizer=none w/ increment > 1 => should honor optimizer
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.NONE.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);

			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( NoopOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 1, generator.getOptimizer().getIncrementSize() );
			assertEquals( 1, generator.getDatabaseStructure().getIncrementSize() );

			// optimizer=hilo w/ increment > 1 => hilo
			props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.HILO.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( HiLoOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 20, generator.getOptimizer().getIncrementSize() );
			assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );

			// optimizer=pooled w/ increment > 1 => hilo
			props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.POOLED.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);
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
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build()) {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );

			props.setProperty( Environment.PREFER_POOLED_VALUES_LO, "true" );
			generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledLoOptimizer.class, generator.getOptimizer().getClass() );

			props.setProperty( Environment.PREFERRED_POOLED_OPTIMIZER, StandardOptimizerDescriptor.POOLED_LOTL.getExternalName() );
			generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
			generator.registerExportables(
					new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
			);
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledLoThreadLocalOptimizer.class, generator.getOptimizer().getClass() );
		}
	}

	public static class TableDialect extends Dialect {
		@Override
		public int getVersion() {
			return 0;
		}
		@Override
		public boolean supportsSequences() {
			return false;
		}
	}

	public static class SequenceDialect extends Dialect {
		@Override
		public int getVersion() {
			return 0;
		}

		@Override
		public boolean supportsSequences() {
			return true;
		}
		@Override
		public boolean supportsPooledSequences() {
			return false;
		}
		@Override
		public String getSequenceNextValString(String sequenceName) throws MappingException {
			return "";
		}
	}

	public static class PooledSequenceDialect extends SequenceDialect {
		public boolean supportsPooledSequences() {
			return true;
		}
	}

}
