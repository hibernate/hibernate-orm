package org.hibernate.test.idgen.enhanced;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.junit.UnitTestCase;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.Hibernate;
import org.hibernate.MappingException;

/**
 * Tests that SequenceStyleGenerator configures itself as expected
 * in various scenarios
 *
 * @author Steve Ebersole
 */
public class SequenceStyleConfigUnitTest extends UnitTestCase {
	public SequenceStyleConfigUnitTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new TestSuite( SequenceStyleConfigUnitTest.class );
	}

	/**
	 * Test all params defaulted with a dialect supporting sequences
	 */
	public void testDefaultedSequenceBackedConfiguration() {
		Dialect dialect = new SequenceDialect();
		Properties props = new Properties();
		SequenceStyleGenerator generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );

		assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.NoopOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
	}

	/**
	 * Test all params defaulted with a dialect which does not support sequences
	 */
	public void testDefaultedTableBackedConfiguration() {
		Dialect dialect = new TableDialect();
		Properties props = new Properties();
		SequenceStyleGenerator generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );

		assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.NoopOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
	}

	/**
	 * Test default optimizer selection for sequence backed generators
	 * based on the configured increment size; both in the case of the
	 * dialect supporting pooled sequences (pooled) and not (hilo)
	 */
	public void testDefaultOptimizerBasedOnIncrementBackedBySequence() {
		Properties props = new Properties();
		props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );

		// for dialects which do not support pooled sequences, we default to hilo
		Dialect dialect = new SequenceDialect();
		SequenceStyleGenerator generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );
		assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.HiLoOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );

		// for dialects which do support pooled sequences, we default to pooled
		dialect = new PooledSequenceDialect();
		generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );
		assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.PooledOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
	}

	/**
	 * Test default optimizer selection for table backed generators
	 * based on the configured increment size.  Here we always prefer
	 * pooled.
	 */
	public void testDefaultOptimizerBasedOnIncrementBackedByTable() {
		Properties props = new Properties();
		props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "10" );
		Dialect dialect = new TableDialect();
		SequenceStyleGenerator generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );
		assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.PooledOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
	}

	/**
	 * Test forcing of table as backing strucuture with dialect supporting sequences
	 */
	public void testForceTableUse() {
		Dialect dialect = new SequenceDialect();
		Properties props = new Properties();
		props.setProperty( SequenceStyleGenerator.FORCE_TBL_PARAM, "true" );
		SequenceStyleGenerator generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );
		assertClassAssignability( TableStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.NoopOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, generator.getDatabaseStructure().getName() );
	}

	/**
	 * Test explicitly specifying both optimizer and increment
	 */
	public void testExplicitOptimizerWithExplicitIncrementSize() {
		// with sequence ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Dialect dialect = new SequenceDialect();

		// optimizer=none w/ increment > 1 => should honor optimizer
		Properties props = new Properties();
		props.setProperty( SequenceStyleGenerator.OPT_PARAM, OptimizerFactory.NONE );
		props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
		SequenceStyleGenerator generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );
		assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.NoopOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( 1, generator.getOptimizer().getIncrementSize() );
		assertEquals( 1, generator.getDatabaseStructure().getIncrementSize() );

		// optimizer=hilo w/ increment > 1 => hilo
		props = new Properties();
		props.setProperty( SequenceStyleGenerator.OPT_PARAM, OptimizerFactory.HILO );
		props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );
		assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.HiLoOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( 20, generator.getOptimizer().getIncrementSize() );
		assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );

		// optimizer=pooled w/ increment > 1 => hilo
		props = new Properties();
		props.setProperty( SequenceStyleGenerator.OPT_PARAM, OptimizerFactory.POOL );
		props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
		generator = new SequenceStyleGenerator();
		generator.configure( Hibernate.LONG, props, dialect );
		assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
		assertClassAssignability( OptimizerFactory.HiLoOptimizer.class, generator.getOptimizer().getClass() );
		assertEquals( 20, generator.getOptimizer().getIncrementSize() );
		assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );
	}

	private static class TableDialect extends Dialect {
		public boolean supportsSequences() {
			return false;
		}
	}

	private static class SequenceDialect extends Dialect {
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

	private static class PooledSequenceDialect extends SequenceDialect {
		public boolean supportsPooledSequences() {
			return true;
		}
	}
}
