/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.data;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Serializables extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SerializableTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		SerializableTestEntity ste = new SerializableTestEntity( new SerObject( "d1" ) );
		em.persist( ste );
		id1 = ste.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		ste = em.find( SerializableTestEntity.class, id1 );
		ste.setObj( new SerObject( "d2" ) );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SerializableTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		SerializableTestEntity ver1 = new SerializableTestEntity( id1, new SerObject( "d1" ) );
		SerializableTestEntity ver2 = new SerializableTestEntity( id1, new SerObject( "d2" ) );

		assert getAuditReader().find( SerializableTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( SerializableTestEntity.class, id1, 2 ).equals( ver2 );
	}
}