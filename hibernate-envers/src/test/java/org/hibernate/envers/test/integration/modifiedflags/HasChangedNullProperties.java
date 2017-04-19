/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.basic.BasicTestEntity1;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedNullProperties extends AbstractModifiedFlagsEntityTest {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity1.class};
	}

	private Integer addNewEntity(String str, long lng) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity1 bte1 = new BasicTestEntity1( str, lng );
		em.persist( bte1 );
		em.getTransaction().commit();

		return bte1.getId();
	}

	private void modifyEntity(Integer id, String str, long lng) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity1 bte1 = em.find( BasicTestEntity1.class, id );
		bte1.setLong1( lng );
		bte1.setStr1( str );
		em.getTransaction().commit();
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = addNewEntity( "x", 1 ); // rev 1
		id2 = addNewEntity( null, 20 ); // rev 2

		modifyEntity( id1, null, 1 ); // rev 3
		modifyEntity( id2, "y2", 20 ); // rev 4
	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChangedWithDeleted(
				BasicTestEntity1.class,
				id1, "str1"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChangedWithDeleted(
				BasicTestEntity1.class,
				id1, "long1"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChangedWithDeleted(
				BasicTestEntity1.class,
				id2, "str1"
		);
		// str1 property was null before insert and after insert so in a way it didn't change - is it a good way to go?
		assertEquals( 1, list.size() );
		assertEquals( makeList( 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChangedWithDeleted(
				BasicTestEntity1.class,
				id2, "long1"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );

		list = getAuditReader().createQuery().forRevisionsOfEntity( BasicTestEntity1.class, false, true )
				.add( AuditEntity.property( "str1" ).hasChanged() )
				.add( AuditEntity.property( "long1" ).hasChanged() )
				.getResultList();
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );
	}
}