/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.inheritance.joined.ChildEntity;
import org.hibernate.envers.test.integration.inheritance.joined.ParentEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedChildAuditing extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildEntity.class, ParentEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		id1 = 1;

		// Rev 1
		em.getTransaction().begin();
		ChildEntity ce = new ChildEntity( id1, "x", 1l );
		em.persist( ce );
		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();
		ce = em.find( ChildEntity.class, id1 );
		ce.setData( "y" );
		ce.setNumVal( 2l );
		em.getTransaction().commit();
	}

	@Test
	public void testChildHasChanged() throws Exception {
		List list = queryForPropertyHasChanged( ChildEntity.class, id1, "data" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ChildEntity.class, id1, "numVal" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged( ChildEntity.class, id1, "data" );
		assertEquals( 0, list.size() );

		list = queryForPropertyHasNotChanged( ChildEntity.class, id1, "numVal" );
		assertEquals( 0, list.size() );
	}

	@Test
	public void testParentHasChanged() throws Exception {
		List list = queryForPropertyHasChanged( ParentEntity.class, id1, "data" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged( ParentEntity.class, id1, "data" );
		assertEquals( 0, list.size() );
	}
}