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
import org.hibernate.envers.test.integration.onetoone.bidirectional.BiRefEdEntity;
import org.hibernate.envers.test.integration.onetoone.bidirectional.BiRefIngEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedBidirectional2 extends AbstractModifiedFlagsEntityTest {
	private Integer ed1_id;
	private Integer ed2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BiRefEdEntity.class, BiRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
		BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

		BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
		BiRefIngEntity ing2 = new BiRefIngEntity( 4, "data_ing_2" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ed1 = em.find( BiRefEdEntity.class, ed1.getId() );

		ing1.setReference( ed1 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ed1 = em.find( BiRefEdEntity.class, ed1.getId() );
		ing1 = em.find( BiRefIngEntity.class, ing1.getId() );
		ing2 = em.find( BiRefIngEntity.class, ing2.getId() );

		ing1.setReference( null );
		ing2.setReference( ed1 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ed2 = em.find( BiRefEdEntity.class, ed2.getId() );
		ing1 = em.find( BiRefIngEntity.class, ing1.getId() );
		ing2 = em.find( BiRefIngEntity.class, ing2.getId() );

		ing1.setReference( ed2 );
		ing2.setReference( null );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				BiRefEdEntity.class, ed1_id,
				"referencing"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 2, 3, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				BiRefEdEntity.class, ed2_id,
				"referencing"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 4 ), extractRevisionNumbers( list ) );
	}
}