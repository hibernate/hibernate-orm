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
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.Component2;
import org.hibernate.envers.test.entities.components.ComponentTestEntity;
import org.hibernate.envers.test.integration.collection.mapkey.ComponentMapKeyEntity;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@FailureExpectedWithNewMetamodel( message = "Plural attribute index that is an attribute of the referenced entity is not supported yet." )
public class HasChangedComponentMapKey extends AbstractModifiedFlagsEntityTest {
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
	public void testHasChangedMapEntity() throws Exception {
		List list = queryForPropertyHasChanged( ComponentMapKeyEntity.class, cmke_id, "idmap" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				ComponentMapKeyEntity.class,
				cmke_id, "idmap"
		);
		assertEquals( 0, list.size() );
	}

	@Test
	public void testHasChangedComponentEntity() throws Exception {
		List list = queryForPropertyHasChanged(
				ComponentTestEntity.class,
				cte1_id, "comp1"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				ComponentTestEntity.class, cte1_id,
				"comp1"
		);
		assertEquals( 0, list.size() );

		list = queryForPropertyHasChanged( ComponentTestEntity.class, cte2_id, "comp1" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged( ComponentTestEntity.class, cte2_id, "comp1" );
		assertEquals( 0, list.size() );
	}
}