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
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Bidirectional2 extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

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

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3, 4 ).equals( getAuditReader().getRevisions( BiRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 4 ).equals( getAuditReader().getRevisions( BiRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 2, 3, 4 ).equals( getAuditReader().getRevisions( BiRefIngEntity.class, ing1_id ) );
		assert Arrays.asList( 2, 3, 4 ).equals( getAuditReader().getRevisions( BiRefIngEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		BiRefIngEntity ing1 = getEntityManager().find( BiRefIngEntity.class, ing1_id );
		BiRefIngEntity ing2 = getEntityManager().find( BiRefIngEntity.class, ing2_id );

		BiRefEdEntity rev1 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 1 );
		BiRefEdEntity rev2 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 2 );
		BiRefEdEntity rev3 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 3 );
		BiRefEdEntity rev4 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 4 );

		assert rev1.getReferencing() == null;
		assert rev2.getReferencing().equals( ing1 );
		assert rev3.getReferencing().equals( ing2 );
		assert rev4.getReferencing() == null;
	}

	@Test
	public void testHistoryOfEdId2() {
		BiRefIngEntity ing1 = getEntityManager().find( BiRefIngEntity.class, ing1_id );

		BiRefEdEntity rev1 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 1 );
		BiRefEdEntity rev2 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 2 );
		BiRefEdEntity rev3 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 3 );
		BiRefEdEntity rev4 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 4 );

		assert rev1.getReferencing() == null;
		assert rev2.getReferencing() == null;
		assert rev3.getReferencing() == null;
		assert rev4.getReferencing().equals( ing1 );
	}
}