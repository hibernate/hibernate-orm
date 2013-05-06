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
package org.hibernate.envers.test.integration.onetoone.unidirectional;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Unidirectional extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;
	private Integer ed3_id;
	private Integer ed4_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {UniRefEdEntity.class, UniRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		UniRefEdEntity ed1 = new UniRefEdEntity( 1, "data_ed_1" );
		UniRefEdEntity ed2 = new UniRefEdEntity( 2, "data_ed_2" );
		UniRefEdEntity ed3 = new UniRefEdEntity( 3, "data_ed_2" );
		UniRefEdEntity ed4 = new UniRefEdEntity( 4, "data_ed_2" );

		UniRefIngEntity ing1 = new UniRefIngEntity( 5, "data_ing_1", ed1 );
		UniRefIngEntity ing2 = new UniRefIngEntity( 6, "data_ing_2", ed3 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ed3 );
		em.persist( ed4 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2

		em = getEntityManager();
		em.getTransaction().begin();

		ing1 = em.find( UniRefIngEntity.class, ing1.getId() );
		ed2 = em.find( UniRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );

		em.getTransaction().commit();

		// Revision 3

		em = getEntityManager();
		em.getTransaction().begin();

		ing2 = em.find( UniRefIngEntity.class, ing2.getId() );
		ed3 = em.find( UniRefEdEntity.class, ed3.getId() );

		ing2.setReference( ed4 );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();
		ed3_id = ed3.getId();
		ed4_id = ed4.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( UniRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( UniRefEdEntity.class, ed2_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( UniRefEdEntity.class, ed3_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( UniRefEdEntity.class, ed4_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( UniRefIngEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( UniRefIngEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfIngId1() {
		UniRefEdEntity ed1 = getEntityManager().find( UniRefEdEntity.class, ed1_id );
		UniRefEdEntity ed2 = getEntityManager().find( UniRefEdEntity.class, ed2_id );

		UniRefIngEntity rev1 = getAuditReader().find( UniRefIngEntity.class, ing1_id, 1 );
		UniRefIngEntity rev2 = getAuditReader().find( UniRefIngEntity.class, ing1_id, 2 );
		UniRefIngEntity rev3 = getAuditReader().find( UniRefIngEntity.class, ing1_id, 3 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed2 );
		assert rev3.getReference().equals( ed2 );
	}

	@Test
	public void testHistoryOfIngId2() {
		UniRefEdEntity ed3 = getEntityManager().find( UniRefEdEntity.class, ed3_id );
		UniRefEdEntity ed4 = getEntityManager().find( UniRefEdEntity.class, ed4_id );

		UniRefIngEntity rev1 = getAuditReader().find( UniRefIngEntity.class, ing2_id, 1 );
		UniRefIngEntity rev2 = getAuditReader().find( UniRefIngEntity.class, ing2_id, 2 );
		UniRefIngEntity rev3 = getAuditReader().find( UniRefIngEntity.class, ing2_id, 3 );

		assert rev1.getReference().equals( ed3 );
		assert rev2.getReference().equals( ed3 );
		assert rev3.getReference().equals( ed4 );
	}
}
