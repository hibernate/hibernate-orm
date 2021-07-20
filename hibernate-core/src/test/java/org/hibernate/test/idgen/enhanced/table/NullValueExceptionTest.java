/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.table;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;

public class NullValueExceptionTest extends BaseCoreFunctionalTestCase {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Override
	public String[] getMappings() {
		return new String[] { "idgen/enhanced/table/Basic.hbm.xml" };
	}

	@Test
	public void testNormalBoundary() {
		expectedException.expect(HibernateException.class);
		expectedException.expectMessage("null 'next_val' for sequence_name 'test'");

		EntityPersister persister = sessionFactory().getEntityPersister( Entity.class.getName() );
		assertClassAssignability( TableGenerator.class, persister.getIdentifierGenerator().getClass() );
		TableGenerator generator = ( TableGenerator ) persister.getIdentifierGenerator();

		Session s = openSession();

		// This situation can only happen via human being or bad
		// migration/clone script. Simulate this record being updated
		// post table generation.
		s.beginTransaction();
		s.createNativeQuery(
			"UPDATE ID_TBL_BSC_TBL SET next_val = null where sequence_name = 'test'"
		).executeUpdate();
		s.getTransaction().commit();

		s.beginTransaction();
		Entity entity = new Entity( "" + 1 );
		s.save( entity );
		s.getTransaction().commit();

		s.close();
	}

}
