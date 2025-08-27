/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E1;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E2;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedEnumSet extends AbstractModifiedFlagsEntityTest {
	private Integer sse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EnumSetEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EnumSetEntity sse1 = new EnumSetEntity();

		// Revision 1 (sse1: initialy 1 element)
		em.getTransaction().begin();

		sse1.getEnums1().add( E1.X );
		sse1.getEnums2().add( E2.A );

		em.persist( sse1 );

		em.getTransaction().commit();

		// Revision 2 (sse1: adding 1 element/removing a non-existing element)
		em.getTransaction().begin();

		sse1 = em.find( EnumSetEntity.class, sse1.getId() );

		sse1.getEnums1().add( E1.Y );
		sse1.getEnums2().remove( E2.B );

		em.getTransaction().commit();

		// Revision 3 (sse1: removing 1 element/adding an exisiting element)
		em.getTransaction().begin();

		sse1 = em.find( EnumSetEntity.class, sse1.getId() );

		sse1.getEnums1().remove( E1.X );
		sse1.getEnums2().add( E2.A );

		em.getTransaction().commit();

		//

		sse1_id = sse1.getId();
	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				EnumSetEntity.class, sse1_id,
				"enums1"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				EnumSetEntity.class, sse1_id,
				"enums2"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				EnumSetEntity.class, sse1_id,
				"enums1"
		);
		assertEquals( 0, list.size() );

		list = queryForPropertyHasNotChanged(
				EnumSetEntity.class, sse1_id,
				"enums2"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );
	}
}
