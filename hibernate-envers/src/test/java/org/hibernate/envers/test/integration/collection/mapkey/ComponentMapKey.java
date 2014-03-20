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
package org.hibernate.envers.test.integration.collection.mapkey;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.Component2;
import org.hibernate.envers.test.entities.components.ComponentTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( message = " Plural attribute index that is an attribute of the referenced entity is not supported yet." )
public class ComponentMapKey extends BaseEnversJPAFunctionalTestCase {
	private Integer cmke_id;

	private Integer cte1_id;
	private Integer cte2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ComponentMapKeyEntity.class, ComponentTestEntity.class, Component1.class, Component2.class};
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