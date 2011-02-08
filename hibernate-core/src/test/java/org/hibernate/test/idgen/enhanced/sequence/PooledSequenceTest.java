package org.hibernate.test.idgen.enhanced.sequence;
import junit.framework.Test;
import org.hibernate.Session;
import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PooledSequenceTest extends FunctionalTestCase {
	public PooledSequenceTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "idgen/enhanced/sequence/Pooled.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PooledSequenceTest.class );
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
			assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() ); // initialization calls seq twice
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization calls seq twice
			assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
		}
		// now force a "clock over"
		entities[ increment ] = new Entity( "" + increment );
		s.save( entities[ increment ] );
		assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() ); // initialization (2) + clock over
		assertEquals( ( increment * 2 ) + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization (2) + clock over
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
