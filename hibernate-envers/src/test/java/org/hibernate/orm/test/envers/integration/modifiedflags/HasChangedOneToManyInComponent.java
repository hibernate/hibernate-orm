/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.components.relations.OneToManyComponent;
import org.hibernate.orm.test.envers.entities.components.relations.OneToManyComponentTestEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedOneToManyInComponent extends AbstractModifiedFlagsEntityTest {
	private Integer otmcte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {OneToManyComponentTestEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrTestEntity ste1 = new StrTestEntity();
		ste1.setStr( "str1" );

		StrTestEntity ste2 = new StrTestEntity();
		ste2.setStr( "str2" );

		em.persist( ste1 );
		em.persist( ste2 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		OneToManyComponentTestEntity otmcte1 = new OneToManyComponentTestEntity( new OneToManyComponent( "data1" ) );
		otmcte1.getComp1().getEntities().add( ste1 );

		em.persist( otmcte1 );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		otmcte1 = em.find( OneToManyComponentTestEntity.class, otmcte1.getId() );
		otmcte1.getComp1().getEntities().add( ste2 );

		em.getTransaction().commit();

		otmcte_id1 = otmcte1.getId();
	}

	@Test
	public void testHasChangedId1() throws Exception {
		List list =
				queryForPropertyHasChanged(
						OneToManyComponentTestEntity.class,
						otmcte_id1, "comp1"
				);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				OneToManyComponentTestEntity.class,
				otmcte_id1, "comp1"
		);
		assertEquals( 0, list.size() );
	}
}
