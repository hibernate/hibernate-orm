package org.hibernate.test.idgen.enhanced.table;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.Session;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PooledTableTest extends FunctionalTestCase {
	public PooledTableTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "idgen/enhanced/table/Pooled.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PooledTableTest.class );
	}

	public void testNormalBoundary() {
		EntityPersister persister = sfi().getEntityPersister( Entity.class.getName() );
		assertClassAssignability( TableGenerator.class, persister.getIdentifierGenerator().getClass() );
		TableGenerator generator = ( TableGenerator ) persister.getIdentifierGenerator();
		assertClassAssignability( OptimizerFactory.PooledOptimizer.class, generator.getOptimizer().getClass() );
		OptimizerFactory.PooledOptimizer optimizer = ( OptimizerFactory.PooledOptimizer ) generator.getOptimizer();

		int increment = optimizer.getIncrementSize();
		Entity[] entities = new Entity[ increment + 1 ];
		Session s = openSession();
		s.beginTransaction();
		for ( int i = 0; i < increment; i++ ) {
			entities[i] = new Entity( "" + ( i + 1 ) );
			s.save( entities[i] );
			assertEquals( 2, generator.getTableAccessCount() ); // initialization calls seq twice
			assertEquals( increment + 1, optimizer.getLastSourceValue() ); // initialization calls seq twice
			assertEquals( i + 1, optimizer.getLastValue() );
			assertEquals( increment + 1, optimizer.getLastSourceValue() );
		}
		// now force a "clock over"
		entities[ increment ] = new Entity( "" + increment );
		s.save( entities[ increment ] );
		assertEquals( 3, generator.getTableAccessCount() ); // initialization (2) + clock over
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
