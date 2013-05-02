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
package org.hibernate.envers.test.integration.manytomany.unidirectional;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.manytomany.unidirectional.SetUniEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicUniSet extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SetUniEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StrTestEntity ed1 = new StrTestEntity( "data_ed_1" );
		StrTestEntity ed2 = new StrTestEntity( "data_ed_2" );

		SetUniEntity ing1 = new SetUniEntity( 3, "data_ing_1" );
		SetUniEntity ing2 = new SetUniEntity( 4, "data_ing_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		ing1 = em.find( SetUniEntity.class, ing1.getId() );
		ing2 = em.find( SetUniEntity.class, ing2.getId() );
		ed1 = em.find( StrTestEntity.class, ed1.getId() );
		ed2 = em.find( StrTestEntity.class, ed2.getId() );

		ing1.setReferences( new HashSet<StrTestEntity>() );
		ing1.getReferences().add( ed1 );

		ing2.setReferences( new HashSet<StrTestEntity>() );
		ing2.getReferences().add( ed1 );
		ing2.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing1 = em.find( SetUniEntity.class, ing1.getId() );
		ed2 = em.find( StrTestEntity.class, ed2.getId() );
		ed1 = em.find( StrTestEntity.class, ed1.getId() );

		ing1.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ing1 = em.find( SetUniEntity.class, ing1.getId() );
		ed2 = em.find( StrTestEntity.class, ed2.getId() );
		ed1 = em.find( StrTestEntity.class, ed1.getId() );

		ing1.getReferences().remove( ed1 );

		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();

		ing1 = em.find( SetUniEntity.class, ing1.getId() );

		ing1.setReferences( null );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, ed1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2, 3, 4, 5 ).equals( getAuditReader().getRevisions( SetUniEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SetUniEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		StrTestEntity ed1 = getEntityManager().find( StrTestEntity.class, ed1_id );
		StrTestEntity ed2 = getEntityManager().find( StrTestEntity.class, ed2_id );

		SetUniEntity rev1 = getAuditReader().find( SetUniEntity.class, ing1_id, 1 );
		SetUniEntity rev2 = getAuditReader().find( SetUniEntity.class, ing1_id, 2 );
		SetUniEntity rev3 = getAuditReader().find( SetUniEntity.class, ing1_id, 3 );
		SetUniEntity rev4 = getAuditReader().find( SetUniEntity.class, ing1_id, 4 );
		SetUniEntity rev5 = getAuditReader().find( SetUniEntity.class, ing1_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_SET );
		assert rev2.getReferences().equals( TestTools.makeSet( ed1 ) );
		assert rev3.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev4.getReferences().equals( TestTools.makeSet( ed2 ) );
		assert rev5.getReferences().equals( Collections.EMPTY_SET );
	}

	@Test
	public void testHistoryOfEdIng2() {
		StrTestEntity ed1 = getEntityManager().find( StrTestEntity.class, ed1_id );
		StrTestEntity ed2 = getEntityManager().find( StrTestEntity.class, ed2_id );

		SetUniEntity rev1 = getAuditReader().find( SetUniEntity.class, ing2_id, 1 );
		SetUniEntity rev2 = getAuditReader().find( SetUniEntity.class, ing2_id, 2 );
		SetUniEntity rev3 = getAuditReader().find( SetUniEntity.class, ing2_id, 3 );
		SetUniEntity rev4 = getAuditReader().find( SetUniEntity.class, ing2_id, 4 );
		SetUniEntity rev5 = getAuditReader().find( SetUniEntity.class, ing2_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_SET );
		assert rev2.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev3.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev4.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev5.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
	}
}