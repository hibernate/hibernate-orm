package org.hibernate.test.idgen.enhanced.forcedtable;

import junit.framework.Test;

import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.Session;

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
		assertClassAssignability( SequenceStyleGenerator.class, persister.getIdentifierGenerator().getClass() );
		SequenceStyleGenerator generator = ( SequenceStyleGenerator ) persister.getIdentifierGenerator();
		assertClassAssignability( OptimizerFactory.PooledOptimizer.class, generator.getOptimizer().getClass() );
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
			assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() ); // initialization calls table twice
			assertEquals( increment + 1, optimizer.getLastSourceValue() ); // initialization calls table twice
			assertEquals( i + 1, optimizer.getLastValue() );
			assertEquals( increment + 1, optimizer.getLastSourceValue() );
		}
		// now force a "clock over"
		entities[ increment ] = new Entity( "" + increment );
		s.save( entities[ increment ] );
		long expectedId = optimizer.getIncrementSize() + 1;
		assertEquals( expectedId, entities[ optimizer.getIncrementSize() ].getId().longValue() );
		assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() ); // initialization (2) + clock over
		assertEquals( ( increment * 2 ) + 1, optimizer.getLastSourceValue() ); // initialization (2) + clock over
		assertEquals( increment + 1, optimizer.getLastValue() );
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
