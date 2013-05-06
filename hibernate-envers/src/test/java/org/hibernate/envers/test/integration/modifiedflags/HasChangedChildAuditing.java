/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

package org.hibernate.envers.test.integration.modifiedflags;

import javax.persistence.EntityManager;
import java.util.List;

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