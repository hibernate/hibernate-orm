/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.table;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class HiLoTableTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "idgen/enhanced/table/HiLo.hbm.xml" };
	}

	@Test
	public void testNormalBoundary() {
		EntityPersister persister = sessionFactory().getEntityPersister( Entity.class.getName() );
		assertClassAssignability( TableGenerator.class, persister.getIdentifierGenerator().getClass() );
		TableGenerator generator = ( TableGenerator ) persister.getIdentifierGenerator();
		assertClassAssignability( HiLoOptimizer.class, generator.getOptimizer().getClass() );
		HiLoOptimizer optimizer = (HiLoOptimizer) generator.getOptimizer();

		int increment = optimizer.getIncrementSize();
		Entity[] entities = new Entity[ increment + 1 ];
		Session s = openSession();
		s.beginTransaction();
		for ( int i = 0; i < increment; i++ ) {
			entities[i] = new Entity( "" + ( i + 1 ) );
			s.save( entities[i] );
			assertEquals( 1, generator.getTableAccessCount() ); // initialization
			assertEquals( 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization
			assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue() );
		}
		// now force a "clock over"
		entities[ increment ] = new Entity( "" + increment );
		s.save( entities[ increment ] );
		assertEquals( 2, generator.getTableAccessCount() ); // initialization
		assertEquals( 2, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization
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
