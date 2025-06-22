/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.collection.StringMapEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedStringMap extends AbstractModifiedFlagsEntityTest {
	private Integer sme1_id;
	private Integer sme2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StringMapEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StringMapEntity sme1 = new StringMapEntity();
		StringMapEntity sme2 = new StringMapEntity();

		// Revision 1 (sme1: initialy empty, sme2: initialy 1 mapping)
		em.getTransaction().begin();

		sme2.getStrings().put( "1", "a" );

		em.persist( sme1 );
		em.persist( sme2 );

		em.getTransaction().commit();

		// Revision 2 (sme1: adding 2 mappings, sme2: no changes)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().put( "1", "a" );
		sme1.getStrings().put( "2", "b" );

		em.getTransaction().commit();

		// Revision 3 (sme1: removing an existing mapping, sme2: replacing a value)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().remove( "1" );
		sme2.getStrings().put( "1", "b" );

		em.getTransaction().commit();

		// No revision (sme1: removing a non-existing mapping, sme2: replacing with the same value)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().remove( "3" );
		sme2.getStrings().put( "1", "b" );

		em.getTransaction().commit();

		//

		sme1_id = sme1.getId();
		sme2_id = sme2.getId();
	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				StringMapEntity.class, sme1_id,
				"strings"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				StringMapEntity.class, sme2_id,
				"strings"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				StringMapEntity.class, sme1_id,
				"strings"
		);
		assertEquals( 0, list.size() );

		list = queryForPropertyHasNotChanged(
				StringMapEntity.class, sme2_id,
				"strings"
		);
		assertEquals( 0, list.size() ); // in rev 2 there was no version generated for sme2_id
	}
}
