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
package org.hibernate.envers.test.integration.onetomany.detached;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefEdChildEntity;
import org.hibernate.envers.test.entities.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefEdParentEntity;
import org.hibernate.envers.test.entities.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefIngEntity;

import org.junit.Test;

import static org.hibernate.envers.test.tools.TestTools.checkCollection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relatin),
 * and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinColumnBidirectionalListWithInheritance extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ListJoinColumnBidirectionalInheritanceRefEdChildEntity.class,
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class
		};
	}

	@Test
	@Priority(10)
	public void createData() {
		EntityManager em = getEntityManager();

		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed1 = new ListJoinColumnBidirectionalInheritanceRefEdChildEntity(
				"ed1",
				null,
				"ed1 child"
		);
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = new ListJoinColumnBidirectionalInheritanceRefEdChildEntity(
				"ed2",
				null,
				"ed2 child"
		);

		ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = new ListJoinColumnBidirectionalInheritanceRefIngEntity(
				"coll1",
				ed1
		);
		ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 = new ListJoinColumnBidirectionalInheritanceRefIngEntity(
				"coll1",
				ed2
		);

		// Revision 1 (ing1: ed1, ing2: ed2)
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2 (ing1: ed1, ed2)
		em.getTransaction().begin();

		ing1 = em.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing1.getId() );
		ing2 = em.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing2.getId() );
		ed1 = em.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed1.getId() );
		ed2 = em.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed2.getId() );

		ing2.getReferences().remove( ed2 );
		ing1.getReferences().add( ed2 );

		em.getTransaction().commit();
		em.clear();

		//

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( 1, 2 ), getAuditReader().getRevisions(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing1_id
		)
		);
		assertEquals(
				Arrays.asList( 1, 2 ), getAuditReader().getRevisions(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing2_id
		)
		);

		assertEquals(
				Arrays.asList( 1 ), getAuditReader().getRevisions(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed1_id
		)
		);
		assertEquals(
				Arrays.asList( 1, 2 ), getAuditReader().getRevisions(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed2_id
		)
		);
	}

	@Test
	public void testHistoryOfIng1() {
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed1 = getEntityManager().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed1_id
		);
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = getEntityManager().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed2_id
		);

		ListJoinColumnBidirectionalInheritanceRefIngEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing1_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefIngEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing1_id,
				2
		);

		assertTrue( checkCollection( rev1.getReferences(), ed1 ) );
		assertTrue( checkCollection( rev2.getReferences(), ed1, ed2 ) );
	}

	@Test
	public void testHistoryOfIng2() {
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = getEntityManager().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed2_id
		);

		ListJoinColumnBidirectionalInheritanceRefIngEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing2_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefIngEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing2_id,
				2
		);

		assertTrue( checkCollection( rev1.getReferences(), ed2 ) );
		assertTrue( checkCollection( rev2.getReferences() ) );
	}

	@Test
	public void testHistoryOfEd1() {
		ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = getEntityManager().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing1_id
		);

		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed1_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed1_id,
				2
		);

		assertTrue( rev1.getOwner().equals( ing1 ) );
		assertTrue( rev2.getOwner().equals( ing1 ) );
	}

	@Test
	public void testHistoryOfEd2() {
		ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = getEntityManager().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing1_id
		);
		ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 = getEntityManager().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing2_id
		);

		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed2_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed2_id,
				2
		);

		assertTrue( rev1.getOwner().equals( ing2 ) );
		assertTrue( rev2.getOwner().equals( ing1 ) );
	}

}