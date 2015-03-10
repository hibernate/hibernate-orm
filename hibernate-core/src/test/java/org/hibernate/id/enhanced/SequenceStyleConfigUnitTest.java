/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id.enhanced;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.common.MetadataBuildingContextTestingImpl;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.assertEquals;

/**
 * Tests that SequenceStyleGenerator configures itself as expected in various scenarios
 *
 * @author Steve Ebersole
 */
public class SequenceStyleConfigUnitTest extends BaseUnitTestCase {

	/**
	 * Test all params defaulted with a dialect supporting sequences
	 */
	@Test
	public void testDefaultedSequenceBackedConfiguration() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build();

		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );

			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( NoopOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	private Properties buildGeneratorPropertiesBase(StandardServiceRegistry serviceRegistry) {
		Properties props = new Properties();
		props.put(
				PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
				new MetadataBuildingContextTestingImpl( serviceRegistry ).getObjectNameNormalizer()
		);
		return props;
	}

	/**
	 * Test all params defaulted with a dialect which does not support sequences
	 */
	@Test
	public void testDefaultedTableBackedConfiguration() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, TableDialect.class.getName() )
				.build();

		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );

			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( NoopOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
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
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build();

		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		// for dialects which do support pooled sequences, we default to pooled+sequence
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build();

		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	/**
	 * Test default optimizer selection for table backed generators
	 * based on the configured increment size.  Here we always prefer
	 * pooled.
	 */
	@Test
	public void testDefaultOptimizerBasedOnIncrementBackedByTable() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, TableDialect.class.getName() )
				.build();

		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	/**
	 * Test forcing of table as backing structure with dialect supporting sequences
	 */
	@Test
	public void testForceTableUse() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build();

		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.FORCE_TBL_PARAM, "true" );

			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( NoopOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	/**
	 * Test explicitly specifying both optimizer and increment
	 */
	@Test
	public void testExplicitOptimizerWithExplicitIncrementSize() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SequenceDialect.class.getName() )
				.build();

		// optimizer=none w/ increment > 1 => should honor optimizer
		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.NONE.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( NoopOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 1, generator.getOptimizer().getIncrementSize() );
			assertEquals( 1, generator.getDatabaseStructure().getIncrementSize() );

			// optimizer=hilo w/ increment > 1 => hilo
			props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.HILO.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( HiLoOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 20, generator.getOptimizer().getIncrementSize() );
			assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );

			// optimizer=pooled w/ increment > 1 => hilo
			props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.POOLED.getExternalName() );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			// because the dialect reports to not support pooled seqyences, the expectation is that we will
			// use a table for the backing structure...
			assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
			assertEquals( 20, generator.getOptimizer().getIncrementSize() );
			assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testPreferPooledLoSettingHonored() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build();

		try {
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
			props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
			SequenceStyleGenerator generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );

			props.setProperty( Environment.PREFER_POOLED_VALUES_LO, "true" );
			generator = new SequenceStyleGenerator();
			generator.configure( StandardBasicTypes.LONG, props, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
			assertClassAssignability( PooledLoOptimizer.class, generator.getOptimizer().getClass() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	public static class TableDialect extends Dialect {
		public boolean supportsSequences() {
			return false;
		}
	}

	public static class SequenceDialect extends Dialect {
		public boolean supportsSequences() {
			return true;
		}
		public boolean supportsPooledSequences() {
			return false;
		}
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
