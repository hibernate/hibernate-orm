/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.forcedtable;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class HiLoForcedTableSequenceTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "idgen/enhanced/forcedtable/HiLo.hbm.xml" };
	}

	@Test
	public void testNormalBoundary() {
		EntityPersister persister = sessionFactory().getEntityPersister( Entity.class.getName() );
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
				"hilo optimizer was not used",
				HiLoOptimizer.class.isInstance( generator.getOptimizer() )
		);
		HiLoOptimizer optimizer = (HiLoOptimizer) generator.getOptimizer();

		int increment = optimizer.getIncrementSize();
		Entity[] entities = new Entity[ increment + 1 ];
		Session s = openSession();
		s.beginTransaction();
		for ( int i = 0; i < increment; i++ ) {
			entities[i] = new Entity( "" + ( i + 1 ) );
			s.save( entities[i] );
			long expectedId = i + 1;
			assertEquals( expectedId, entities[i].getId().longValue() );
			assertEquals( 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
			assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue() );
		}
		// now force a "clock over"
		entities[ increment ] = new Entity( "" + increment );
		s.save( entities[ increment ] );
		long expectedId = optimizer.getIncrementSize() + 1;
		assertEquals( expectedId, entities[ optimizer.getIncrementSize() ].getId().longValue() );
		assertEquals( 2, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization + clock-over
		assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
		assertEquals( ( increment * 2 ) + 1, ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue() );

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
