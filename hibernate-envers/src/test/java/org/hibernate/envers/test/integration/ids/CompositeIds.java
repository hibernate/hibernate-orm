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
package org.hibernate.envers.test.integration.ids;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.CustomEnum;
import org.hibernate.envers.test.entities.ids.EmbId;
import org.hibernate.envers.test.entities.ids.EmbIdTestEntity;
import org.hibernate.envers.test.entities.ids.EmbIdWithCustomType;
import org.hibernate.envers.test.entities.ids.EmbIdWithCustomTypeTestEntity;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.envers.test.entities.ids.MulIdTestEntity;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( message = "Custom types not supported by envers yet." )
public class CompositeIds extends BaseEnversJPAFunctionalTestCase {
	private EmbId id1;
	private EmbId id2;
	private MulId id3;
	private MulId id4;
	private EmbIdWithCustomType id5;
	private EmbIdWithCustomType id6;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EmbIdTestEntity.class,
				MulIdTestEntity.class,
				EmbIdWithCustomTypeTestEntity.class,
				EmbId.class,
				EmbIdWithCustomType.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = new EmbId( 1, 2 );
		id2 = new EmbId( 10, 20 );
		id3 = new MulId( 100, 101 );
		id4 = new MulId( 102, 103 );
		id5 = new EmbIdWithCustomType( 25, CustomEnum.NO );
		id6 = new EmbIdWithCustomType( 27, CustomEnum.YES );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( new EmbIdTestEntity( id1, "x" ) );
		em.persist( new MulIdTestEntity( id3.getId1(), id3.getId2(), "a" ) );
		em.persist( new EmbIdWithCustomTypeTestEntity( id5, "c" ) );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		em.persist( new EmbIdTestEntity( id2, "y" ) );
		em.persist( new MulIdTestEntity( id4.getId1(), id4.getId2(), "b" ) );
		em.persist( new EmbIdWithCustomTypeTestEntity( id6, "d" ) );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		EmbIdTestEntity ete1 = em.find( EmbIdTestEntity.class, id1 );
		EmbIdTestEntity ete2 = em.find( EmbIdTestEntity.class, id2 );
		MulIdTestEntity mte3 = em.find( MulIdTestEntity.class, id3 );
		MulIdTestEntity mte4 = em.find( MulIdTestEntity.class, id4 );
		EmbIdWithCustomTypeTestEntity cte5 = em.find( EmbIdWithCustomTypeTestEntity.class, id5 );
		EmbIdWithCustomTypeTestEntity cte6 = em.find( EmbIdWithCustomTypeTestEntity.class, id6 );

		ete1.setStr1( "x2" );
		ete2.setStr1( "y2" );
		mte3.setStr1( "a2" );
		mte4.setStr1( "b2" );
		cte5.setStr1( "c2" );
		cte6.setStr1( "d2" );

		em.getTransaction().commit();

		// Revision 4
		em = getEntityManager();
		em.getTransaction().begin();

		ete1 = em.find( EmbIdTestEntity.class, id1 );
		ete2 = em.find( EmbIdTestEntity.class, id2 );
		mte3 = em.find( MulIdTestEntity.class, id3 );
		cte5 = em.find( EmbIdWithCustomTypeTestEntity.class, id5 );
		cte6 = em.find( EmbIdWithCustomTypeTestEntity.class, id6 );

		em.remove( ete1 );
		em.remove( mte3 );
		em.remove( cte6 );

		ete2.setStr1( "y3" );
		cte5.setStr1( "c3" );

		em.getTransaction().commit();

		// Revision 5
		em = getEntityManager();
		em.getTransaction().begin();

		ete2 = em.find( EmbIdTestEntity.class, id2 );

		em.remove( ete2 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 3, 4 ).equals( getAuditReader().getRevisions( EmbIdTestEntity.class, id1 ) );

		assert Arrays.asList( 2, 3, 4, 5 ).equals( getAuditReader().getRevisions( EmbIdTestEntity.class, id2 ) );

		assert Arrays.asList( 1, 3, 4 ).equals( getAuditReader().getRevisions( MulIdTestEntity.class, id3 ) );

		assert Arrays.asList( 2, 3 ).equals( getAuditReader().getRevisions( MulIdTestEntity.class, id4 ) );

		assert Arrays.asList( 1, 3, 4 ).equals(
				getAuditReader().getRevisions(
						EmbIdWithCustomTypeTestEntity.class,
						id5
				)
		);

		assert Arrays.asList( 2, 3, 4 ).equals(
				getAuditReader().getRevisions(
						EmbIdWithCustomTypeTestEntity.class,
						id6
				)
		);
	}

	@Test
	public void testHistoryOfId1() {
		EmbIdTestEntity ver1 = new EmbIdTestEntity( id1, "x" );
		EmbIdTestEntity ver2 = new EmbIdTestEntity( id1, "x2" );

		assert getAuditReader().find( EmbIdTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( EmbIdTestEntity.class, id1, 2 ).equals( ver1 );
		assert getAuditReader().find( EmbIdTestEntity.class, id1, 3 ).equals( ver2 );
		assert getAuditReader().find( EmbIdTestEntity.class, id1, 4 ) == null;
		assert getAuditReader().find( EmbIdTestEntity.class, id1, 5 ) == null;
	}

	@Test
	public void testHistoryOfId2() {
		EmbIdTestEntity ver1 = new EmbIdTestEntity( id2, "y" );
		EmbIdTestEntity ver2 = new EmbIdTestEntity( id2, "y2" );
		EmbIdTestEntity ver3 = new EmbIdTestEntity( id2, "y3" );

		assert getAuditReader().find( EmbIdTestEntity.class, id2, 1 ) == null;
		assert getAuditReader().find( EmbIdTestEntity.class, id2, 2 ).equals( ver1 );
		assert getAuditReader().find( EmbIdTestEntity.class, id2, 3 ).equals( ver2 );
		assert getAuditReader().find( EmbIdTestEntity.class, id2, 4 ).equals( ver3 );
		assert getAuditReader().find( EmbIdTestEntity.class, id2, 5 ) == null;
	}

	@Test
	public void testHistoryOfId3() {
		MulIdTestEntity ver1 = new MulIdTestEntity( id3.getId1(), id3.getId2(), "a" );
		MulIdTestEntity ver2 = new MulIdTestEntity( id3.getId1(), id3.getId2(), "a2" );

		assert getAuditReader().find( MulIdTestEntity.class, id3, 1 ).equals( ver1 );
		assert getAuditReader().find( MulIdTestEntity.class, id3, 2 ).equals( ver1 );
		assert getAuditReader().find( MulIdTestEntity.class, id3, 3 ).equals( ver2 );
		assert getAuditReader().find( MulIdTestEntity.class, id3, 4 ) == null;
		assert getAuditReader().find( MulIdTestEntity.class, id3, 5 ) == null;
	}

	@Test
	public void testHistoryOfId4() {
		MulIdTestEntity ver1 = new MulIdTestEntity( id4.getId1(), id4.getId2(), "b" );
		MulIdTestEntity ver2 = new MulIdTestEntity( id4.getId1(), id4.getId2(), "b2" );

		assert getAuditReader().find( MulIdTestEntity.class, id4, 1 ) == null;
		assert getAuditReader().find( MulIdTestEntity.class, id4, 2 ).equals( ver1 );
		assert getAuditReader().find( MulIdTestEntity.class, id4, 3 ).equals( ver2 );
		assert getAuditReader().find( MulIdTestEntity.class, id4, 4 ).equals( ver2 );
		assert getAuditReader().find( MulIdTestEntity.class, id4, 5 ).equals( ver2 );
	}

	@Test
	public void testHistoryOfId5() {
		EmbIdWithCustomTypeTestEntity ver1 = new EmbIdWithCustomTypeTestEntity( id5, "c" );
		EmbIdWithCustomTypeTestEntity ver2 = new EmbIdWithCustomTypeTestEntity( id5, "c2" );
		EmbIdWithCustomTypeTestEntity ver3 = new EmbIdWithCustomTypeTestEntity( id5, "c3" );

		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 1 ).equals( ver1 );
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 2 ).equals( ver1 );
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 3 ).equals( ver2 );
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 4 ).equals( ver3 );
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 5 ).equals( ver3 );
	}

	@Test
	public void testHistoryOfId6() {
		EmbIdWithCustomTypeTestEntity ver1 = new EmbIdWithCustomTypeTestEntity( id6, "d" );
		EmbIdWithCustomTypeTestEntity ver2 = new EmbIdWithCustomTypeTestEntity( id6, "d2" );

		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 1 ) == null;
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 2 ).equals( ver1 );
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 3 ).equals( ver2 );
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 4 ) == null;
		assert getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 5 ) == null;
	}
}
