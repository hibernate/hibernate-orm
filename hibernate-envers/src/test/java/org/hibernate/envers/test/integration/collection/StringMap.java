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
package org.hibernate.envers.test.integration.collection;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.StringMapEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class StringMap extends BaseEnversJPAFunctionalTestCase {
	private Integer sme1_id;
	private Integer sme2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StringMapEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StringMapEntity sme1 = new StringMapEntity();
		StringMapEntity sme2 = new StringMapEntity();

		// Revision 1 (sme1: initialy empty, sme2: initialy 1 mapping)
		em.getTransaction().begin();

		sme2.getStrings().put( "1", "a" );

		em.persist( sme1 );
		em.persist( sme2 );

		em.getTransaction().commit();

		// Revision 2 (sme1: adding 2 mappings, sme2: no changes)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().put( "1", "a" );
		sme1.getStrings().put( "2", "b" );

		em.getTransaction().commit();

		// Revision 3 (sme1: removing an existing mapping, sme2: replacing a value)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().remove( "1" );
		sme2.getStrings().put( "1", "b" );

		em.getTransaction().commit();

		// No revision (sme1: removing a non-existing mapping, sme2: replacing with the same value)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().remove( "3" );
		sme2.getStrings().put( "1", "b" );

		em.getTransaction().commit();

		//

		sme1_id = sme1.getId();
		sme2_id = sme2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( StringMapEntity.class, sme1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( StringMapEntity.class, sme2_id ) );
	}

	@Test
	public void testHistoryOfSse1() {
		StringMapEntity rev1 = getAuditReader().find( StringMapEntity.class, sme1_id, 1 );
		StringMapEntity rev2 = getAuditReader().find( StringMapEntity.class, sme1_id, 2 );
		StringMapEntity rev3 = getAuditReader().find( StringMapEntity.class, sme1_id, 3 );
		StringMapEntity rev4 = getAuditReader().find( StringMapEntity.class, sme1_id, 4 );

		assert rev1.getStrings().equals( Collections.EMPTY_MAP );
		assert rev2.getStrings().equals( TestTools.makeMap( "1", "a", "2", "b" ) );
		assert rev3.getStrings().equals( TestTools.makeMap( "2", "b" ) );
		assert rev4.getStrings().equals( TestTools.makeMap( "2", "b" ) );
	}

	@Test
	public void testHistoryOfSse2() {
		StringMapEntity rev1 = getAuditReader().find( StringMapEntity.class, sme2_id, 1 );
		StringMapEntity rev2 = getAuditReader().find( StringMapEntity.class, sme2_id, 2 );
		StringMapEntity rev3 = getAuditReader().find( StringMapEntity.class, sme2_id, 3 );
		StringMapEntity rev4 = getAuditReader().find( StringMapEntity.class, sme2_id, 4 );

		assert rev1.getStrings().equals( TestTools.makeMap( "1", "a" ) );
		assert rev2.getStrings().equals( TestTools.makeMap( "1", "a" ) );
		assert rev3.getStrings().equals( TestTools.makeMap( "1", "b" ) );
		assert rev4.getStrings().equals( TestTools.makeMap( "1", "b" ) );
	}
}