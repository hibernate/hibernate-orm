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
import org.hibernate.envers.test.integration.inheritance.joined.childrelation.ChildIngEntity;
import org.hibernate.envers.test.integration.inheritance.joined.childrelation.ParentNotIngEntity;
import org.hibernate.envers.test.integration.inheritance.joined.childrelation.ReferencedEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedChildReferencing extends AbstractModifiedFlagsEntityTest {
	private Integer re_id1;
	private Integer re_id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildIngEntity.class, ParentNotIngEntity.class, ReferencedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		re_id1 = 1;
		re_id2 = 10;
		Integer c_id = 100;

		// Rev 1
		em.getTransaction().begin();

		ReferencedEntity re1 = new ReferencedEntity( re_id1 );
		em.persist( re1 );

		ReferencedEntity re2 = new ReferencedEntity( re_id2 );
		em.persist( re2 );

		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();

		re1 = em.find( ReferencedEntity.class, re_id1 );

		ChildIngEntity cie = new ChildIngEntity( c_id, "y", 1l );
		cie.setReferenced( re1 );
		em.persist( cie );
		c_id = cie.getId();

		em.getTransaction().commit();

		// Rev 3
		em.getTransaction().begin();

		re2 = em.find( ReferencedEntity.class, re_id2 );
		cie = em.find( ChildIngEntity.class, c_id );

		cie.setReferenced( re2 );

		em.getTransaction().commit();
	}

	@Test
	public void testReferencedEntityHasChanged() throws Exception {
		List list = queryForPropertyHasChanged( ReferencedEntity.class, re_id1, "referencing" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged( ReferencedEntity.class, re_id1, "referencing" );
		assertEquals( 1, list.size() ); // initially referencing collection is null
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ReferencedEntity.class, re_id2, "referencing" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );
	}

}