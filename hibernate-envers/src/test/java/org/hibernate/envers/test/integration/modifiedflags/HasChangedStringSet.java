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
import org.hibernate.envers.test.entities.collection.StringSetEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedStringSet extends AbstractModifiedFlagsEntityTest {
	private Integer sse1_id;
	private Integer sse2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StringSetEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StringSetEntity sse1 = new StringSetEntity();
		StringSetEntity sse2 = new StringSetEntity();

		// Revision 1 (sse1: initialy empty, sse2: initialy 2 elements)
		em.getTransaction().begin();

		sse2.getStrings().add( "sse2_string1" );
		sse2.getStrings().add( "sse2_string2" );

		em.persist( sse1 );
		em.persist( sse2 );

		em.getTransaction().commit();

		// Revision 2 (sse1: adding 2 elements, sse2: adding an existing element)
		em.getTransaction().begin();

		sse1 = em.find( StringSetEntity.class, sse1.getId() );
		sse2 = em.find( StringSetEntity.class, sse2.getId() );

		sse1.getStrings().add( "sse1_string1" );
		sse1.getStrings().add( "sse1_string2" );

		sse2.getStrings().add( "sse2_string1" );

		em.getTransaction().commit();

		// Revision 3 (sse1: removing a non-existing element, sse2: removing one element)
		em.getTransaction().begin();

		sse1 = em.find( StringSetEntity.class, sse1.getId() );
		sse2 = em.find( StringSetEntity.class, sse2.getId() );

		sse1.getStrings().remove( "sse1_string3" );
		sse2.getStrings().remove( "sse2_string1" );

		em.getTransaction().commit();

		//

		sse1_id = sse1.getId();
		sse2_id = sse2.getId();
	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				StringSetEntity.class, sse1_id,
				"strings"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				StringSetEntity.class, sse2_id,
				"strings"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				StringSetEntity.class, sse1_id,
				"strings"
		);
		assertEquals( 0, list.size() );

		list = queryForPropertyHasNotChanged(
				StringSetEntity.class, sse2_id,
				"strings"
		);
		assertEquals( 0, list.size() );
	}
}