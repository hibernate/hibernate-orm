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
package org.hibernate.envers.test.integration.onetoone.bidirectional.ids;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( jiraKey = "HHH-9055 : Association with an entity with @IdClass is broken." )
public class MulIdBidirectional extends BaseEnversJPAFunctionalTestCase {
	private MulId ed1_id;
	private MulId ed2_id;

	private MulId ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BiMulIdRefEdEntity.class, BiMulIdRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		ed1_id = new MulId( 1, 2 );
		ed2_id = new MulId( 3, 4 );

		ing1_id = new MulId( 5, 6 );

		BiMulIdRefEdEntity ed1 = new BiMulIdRefEdEntity( ed1_id.getId1(), ed1_id.getId2(), "data_ed_1" );
		BiMulIdRefEdEntity ed2 = new BiMulIdRefEdEntity( ed2_id.getId1(), ed2_id.getId2(), "data_ed_2" );

		BiMulIdRefIngEntity ing1 = new BiMulIdRefIngEntity( ing1_id.getId1(), ing1_id.getId2(), "data_ing_1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ing1.setReference( ed1 );

		em.persist( ed1 );
		em.persist( ed2 );

		em.persist( ing1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find( BiMulIdRefIngEntity.class, ing1_id );
		ed2 = em.find( BiMulIdRefEdEntity.class, ed2_id );

		ing1.setReference( ed2 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiMulIdRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiMulIdRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiMulIdRefIngEntity.class, ing1_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		BiMulIdRefIngEntity ing1 = getEntityManager().find( BiMulIdRefIngEntity.class, ing1_id );

		BiMulIdRefEdEntity rev1 = getAuditReader().find( BiMulIdRefEdEntity.class, ed1_id, 1 );
		BiMulIdRefEdEntity rev2 = getAuditReader().find( BiMulIdRefEdEntity.class, ed1_id, 2 );

		assert rev1.getReferencing().equals( ing1 );
		assert rev2.getReferencing() == null;
	}

	@Test
	public void testHistoryOfEdId2() {
		BiMulIdRefIngEntity ing1 = getEntityManager().find( BiMulIdRefIngEntity.class, ing1_id );

		BiMulIdRefEdEntity rev1 = getAuditReader().find( BiMulIdRefEdEntity.class, ed2_id, 1 );
		BiMulIdRefEdEntity rev2 = getAuditReader().find( BiMulIdRefEdEntity.class, ed2_id, 2 );

		assert rev1.getReferencing() == null;
		assert rev2.getReferencing().equals( ing1 );
	}

	@Test
	public void testHistoryOfIngId1() {
		BiMulIdRefEdEntity ed1 = getEntityManager().find( BiMulIdRefEdEntity.class, ed1_id );
		BiMulIdRefEdEntity ed2 = getEntityManager().find( BiMulIdRefEdEntity.class, ed2_id );

		BiMulIdRefIngEntity rev1 = getAuditReader().find( BiMulIdRefIngEntity.class, ing1_id, 1 );
		BiMulIdRefIngEntity rev2 = getAuditReader().find( BiMulIdRefIngEntity.class, ing1_id, 2 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed2 );
	}
}