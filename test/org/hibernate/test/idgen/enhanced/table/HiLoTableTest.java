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
public class HiLoTableTest extends FunctionalTestCase {
	public HiLoTableTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "idgen/enhanced/table/HiLo.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( HiLoTableTest.class );
	}

	public void testNormalBoundary() {
		EntityPersister persister = sfi().getEntityPersister( Entity.class.getName() );
		assertClassAssignability( TableGenerator.class, persister.getIdentifierGenerator().getClass() );
		TableGenerator generator = ( TableGenerator ) persister.getIdentifierGenerator();
		assertClassAssignability( OptimizerFactory.HiLoOptimizer.class, generator.getOptimizer().getClass() );
		OptimizerFactory.HiLoOptimizer optimizer = ( OptimizerFactory.HiLoOptimizer ) generator.getOptimizer();

		int increment = optimizer.getIncrementSize();
		Entity[] entities = new Entity[ increment + 1 ];
		Session s = openSession();
		s.beginTransaction();
		for ( int i = 0; i < increment; i++ ) {
			entities[i] = new Entity( "" + ( i + 1 ) );
			s.save( entities[i] );
			assertEquals( 1, generator.getTableAccessCount() ); // initialization
			assertEquals( 1, optimizer.getLastSourceValue() ); // initialization
			assertEquals( i + 1, optimizer.getLastValue() );
			assertEquals( increment + 1, optimizer.getHiValue() );
		}
		// now force a "clock over"
		entities[ increment ] = new Entity( "" + increment );
		s.save( entities[ increment ] );
		assertEquals( 2, generator.getTableAccessCount() ); // initialization
		assertEquals( 2, optimizer.getLastSourceValue() ); // initialization
		assertEquals( increment + 1, optimizer.getLastValue() );
		assertEquals( ( increment * 2 ) + 1, optimizer.getHiValue() );

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
