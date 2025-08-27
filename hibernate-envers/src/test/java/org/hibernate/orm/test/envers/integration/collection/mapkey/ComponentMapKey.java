/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.mapkey;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.orm.test.envers.entities.components.Component2;
import org.hibernate.orm.test.envers.entities.components.ComponentTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ComponentMapKey extends BaseEnversJPAFunctionalTestCase {
	private Integer cmke_id;

	private Integer cte1_id;
	private Integer cte2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ComponentMapKeyEntity.class, ComponentTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		ComponentMapKeyEntity imke = new ComponentMapKeyEntity();

		// Revision 1 (intialy 1 mapping)
		em.getTransaction().begin();

		ComponentTestEntity cte1 = new ComponentTestEntity(
				new Component1( "x1", "y2" ), new Component2(
				"a1",
				"b2"
		)
		);
		ComponentTestEntity cte2 = new ComponentTestEntity(
				new Component1( "x1", "y2" ), new Component2(
				"a1",
				"b2"
		)
		);

		em.persist( cte1 );
		em.persist( cte2 );

		imke.getIdmap().put( cte1.getComp1(), cte1 );

		em.persist( imke );

		em.getTransaction().commit();

		// Revision 2 (sse1: adding 1 mapping)
		em.getTransaction().begin();

		cte2 = em.find( ComponentTestEntity.class, cte2.getId() );
		imke = em.find( ComponentMapKeyEntity.class, imke.getId() );

		imke.getIdmap().put( cte2.getComp1(), cte2 );

		em.getTransaction().commit();

		//

		cmke_id = imke.getId();

		cte1_id = cte1.getId();
		cte2_id = cte2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ComponentMapKeyEntity.class, cmke_id ) );
	}

	@Test
	public void testHistoryOfImke() {
		ComponentTestEntity cte1 = getEntityManager().find( ComponentTestEntity.class, cte1_id );
		ComponentTestEntity cte2 = getEntityManager().find( ComponentTestEntity.class, cte2_id );

		// These fields are unversioned.
		cte1.setComp2( null );
		cte2.setComp2( null );

		ComponentMapKeyEntity rev1 = getAuditReader().find( ComponentMapKeyEntity.class, cmke_id, 1 );
		ComponentMapKeyEntity rev2 = getAuditReader().find( ComponentMapKeyEntity.class, cmke_id, 2 );

		assert rev1.getIdmap().equals( TestTools.makeMap( cte1.getComp1(), cte1 ) );
		assert rev2.getIdmap().equals( TestTools.makeMap( cte1.getComp1(), cte1, cte2.getComp1(), cte2 ) );
	}
}
