/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.sequence;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class PooledSequenceTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "idgen/enhanced/sequence/Pooled.hbm.xml" };
	}

	@Test
	public void testNormalBoundary() {
		EntityPersister persister = sessionFactory().getEntityPersister( Entity.class.getName() );
		assertClassAssignability( SequenceStyleGenerator.class, persister.getIdentifierGenerator().getClass() );
		SequenceStyleGenerator generator = ( SequenceStyleGenerator ) persister.getIdentifierGenerator();
		assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
		PooledOptimizer optimizer = (PooledOptimizer) generator.getOptimizer();

		int increment = optimizer.getIncrementSize();
		Entity[] entities = new Entity[ increment + 2 ];
		Session s = openSession();
		s.beginTransaction();
		for ( int i = 0; i <= increment; i++ ) {
			entities[i] = new Entity( "" + ( i + 1 ) );
			s.save( entities[i] );
			assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() ); // initialization calls seq twice
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization calls seq twice
			assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
		}
		// now force a "clock over"
		entities[ increment + 1 ] = new Entity( "" + increment );
		s.save( entities[ increment + 1 ] );
		assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() ); // initialization (2) + clock over
		assertEquals( ( increment * 2 ) + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization (2) + clock over
		assertEquals( increment + 2, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
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
