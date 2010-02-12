package org.hibernate.test.idgen.enhanced.forcedtable;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class BasicForcedTableSequenceTest extends DatabaseSpecificFunctionalTestCase {
	public BasicForcedTableSequenceTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "idgen/enhanced/forcedtable/Basic.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BasicForcedTableSequenceTest.class );
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
				"no-op optimizer was not used",
				OptimizerFactory.NoopOptimizer.class.isInstance( generator.getOptimizer() )
		);

		int count = 5;
		Entity[] entities = new Entity[count];
		Session s = openSession();
		s.beginTransaction();
		for ( int i = 0; i < count; i++ ) {
			entities[i] = new Entity( "" + ( i + 1 ) );
			s.save( entities[i] );
			long expectedId = i + 1;
			assertEquals( expectedId, entities[i].getId().longValue() );
			assertEquals( expectedId, generator.getDatabaseStructure().getTimesAccessed() );
			assertEquals( expectedId, ( (BasicHolder) generator.getOptimizer().getLastSourceValue() ).getActualLongValue() );
		}
		s.getTransaction().commit();

		s.beginTransaction();
		for ( int i = 0; i < count; i++ ) {
			assertEquals( i + 1, entities[i].getId().intValue() );
			s.delete( entities[i] );
		}
		s.getTransaction().commit();
		s.close();

	}
}
