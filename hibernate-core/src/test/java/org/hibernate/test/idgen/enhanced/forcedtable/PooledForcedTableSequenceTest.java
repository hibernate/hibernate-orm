package org.hibernate.test.idgen.enhanced.forcedtable;
import junit.framework.Test;
import org.hibernate.Session;
import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PooledForcedTableSequenceTest extends DatabaseSpecificFunctionalTestCase {
	public PooledForcedTableSequenceTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "idgen/enhanced/forcedtable/Pooled.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PooledForcedTableSequenceTest.class );
	}

	public void testNormalBoundary() {
		EntityPersister persister = sfi().getEntityPersister( Entity.class.getName() );
		assertTrue(
				"sequence style generator was not used",
				SequenceStyleGenerator.class.isInstance( persister.getIdentifierGenerator() )
		);
		SequenceStyleGenerator generator = ( SequenceStyleGenerator ) persister.getIdentifierGenerator();
		assertTrue(
				"table structure was not used",
				TableStructure.class.isInstance( generator.getDatabaseStructure() )
		);
		assertTrue(
				"pooled optimizer was not used",
				OptimizerFactory.PooledOptimizer.class.isInstance( generator.getOptimizer() )
		);
		OptimizerFactory.PooledOptimizer optimizer = ( OptimizerFactory.PooledOptimizer ) generator.getOptimizer();

		int increment = optimizer.getIncrementSize();
		Entity[] entities = new Entity[ increment + 1 ];
		Session s = openSession();
		s.beginTransaction();
		for ( int i = 0; i < increment; i++ ) {
			entities[i] = new Entity( "" + ( i + 1 ) );
			s.save( entities[i] );
			long expectedId = i + 1;
			assertEquals( expectedId, entities[i].getId().longValue() );
			// NOTE : initialization calls table twice
			assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() );
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
			assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
		}
		// now force a "clock over"
		entities[ increment ] = new Entity( "" + increment );
		s.save( entities[ increment ] );
		long expectedId = optimizer.getIncrementSize() + 1;
		assertEquals( expectedId, entities[ optimizer.getIncrementSize() ].getId().longValue() );
		// initialization (2) + clock over
		assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() );
		assertEquals( ( increment * 2 ) + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
		assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
		s.getTransaction().commit();

		s.beginTransaction();
		for ( int i = 0; i < entities.length; i++ ) {
			assertEquals( i + 1, entities[i].getId().intValue() );
			s.delete( entities[i] );
		}
		s.getTransaction().commit();
		s.close();
	}
}
