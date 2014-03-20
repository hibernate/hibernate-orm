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
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponent;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponentTestEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedManyToOneInComponent extends AbstractModifiedFlagsEntityTest {
	private Integer mtocte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ManyToOneComponentTestEntity.class, StrTestEntity.class, ManyToOneComponent.class};
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

		ManyToOneComponentTestEntity mtocte1 = new ManyToOneComponentTestEntity(
				new ManyToOneComponent(
						ste1,
						"data1"
				)
		);

		em.persist( mtocte1 );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		mtocte1 = em.find( ManyToOneComponentTestEntity.class, mtocte1.getId() );
		mtocte1.getComp1().setEntity( ste2 );

		em.getTransaction().commit();

		mtocte_id1 = mtocte1.getId();
	}

	@Test
	public void testHasChangedId1() throws Exception {
		List list = queryForPropertyHasChanged(
				ManyToOneComponentTestEntity.class,
				mtocte_id1, "comp1"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				ManyToOneComponentTestEntity.class,
				mtocte_id1, "comp1"
		);
		assertEquals( 0, list.size() );
	}

}
