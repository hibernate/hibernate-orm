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
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Steve Ebersole
 */
public class BasicForcedTableSequenceTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "idgen/enhanced/forcedtable/Basic.hbm.xml" };
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
				"no-op optimizer was not used",
				NoopOptimizer.class.isInstance( generator.getOptimizer() )
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
