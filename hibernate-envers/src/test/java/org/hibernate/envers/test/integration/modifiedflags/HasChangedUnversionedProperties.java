/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.QueryException;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.basic.BasicTestEntity2;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedUnversionedProperties extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity2.class};
	}

	private Integer addNewEntity(String str1, String str2) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity2 bte2 = new BasicTestEntity2( str1, str2 );
		em.persist( bte2 );
		em.getTransaction().commit();

		return bte2.getId();
	}

	private void modifyEntity(Integer id, String str1, String str2) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity2 bte2 = em.find( BasicTestEntity2.class, id );
		bte2.setStr1( str1 );
		bte2.setStr2( str2 );
		em.getTransaction().commit();
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = addNewEntity( "x", "a" ); // rev 1
		modifyEntity( id1, "x", "a" ); // no rev
		modifyEntity( id1, "y", "b" ); // rev 2
		modifyEntity( id1, "y", "c" ); // no rev
	}

	@Test
	public void testHasChangedQuery() throws Exception {
		List list = queryForPropertyHasChanged(
				BasicTestEntity2.class,
				id1, "str1"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );
	}

	@Test(expected = QueryException.class)
	public void testExceptionOnHasChangedQuery() throws Exception {
		queryForPropertyHasChangedWithDeleted(
				BasicTestEntity2.class,
				id1, "str2"
		);
	}
}
