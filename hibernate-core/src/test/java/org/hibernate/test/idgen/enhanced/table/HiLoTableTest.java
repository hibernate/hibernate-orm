/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
