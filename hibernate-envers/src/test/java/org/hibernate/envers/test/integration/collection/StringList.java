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
import org.hibernate.envers.test.entities.collection.StringListEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class StringList extends BaseEnversJPAFunctionalTestCase {
	private Integer sle1_id;
	private Integer sle2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StringListEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StringListEntity sle1 = new StringListEntity();
		StringListEntity sle2 = new StringListEntity();

		// Revision 1 (sle1: initialy empty, sle2: initialy 2 elements)
		em.getTransaction().begin();

		sle2.getStrings().add( "sle2_string1" );
		sle2.getStrings().add( "sle2_string2" );

		em.persist( sle1 );
		em.persist( sle2 );

		em.getTransaction().commit();

		// Revision 2 (sle1: adding 2 elements, sle2: adding an existing element)
		em.getTransaction().begin();

		sle1 = em.find( StringListEntity.class, sle1.getId() );
		sle2 = em.find( StringListEntity.class, sle2.getId() );

		sle1.getStrings().add( "sle1_string1" );
		sle1.getStrings().add( "sle1_string2" );

		sle2.getStrings().add( "sle2_string1" );

		em.getTransaction().commit();

		// Revision 3 (sle1: replacing an element at index 0, sle2: removing an element at index 0)
		em.getTransaction().begin();

		sle1 = em.find( StringListEntity.class, sle1.getId() );
		sle2 = em.find( StringListEntity.class, sle2.getId() );

		sle1.getStrings().set( 0, "sle1_string3" );

		sle2.getStrings().remove( 0 );

		em.getTransaction().commit();

		//

		sle1_id = sle1.getId();
		sle2_id = sle2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( StringListEntity.class, sle1_id ) );
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( StringListEntity.class, sle2_id ) );
	}

	@Test
	public void testHistoryOfSle1() {
		StringListEntity rev1 = getAuditReader().find( StringListEntity.class, sle1_id, 1 );
		StringListEntity rev2 = getAuditReader().find( StringListEntity.class, sle1_id, 2 );
		StringListEntity rev3 = getAuditReader().find( StringListEntity.class, sle1_id, 3 );

		assert rev1.getStrings().equals( Collections.EMPTY_LIST );
		assert rev2.getStrings().equals( TestTools.makeList( "sle1_string1", "sle1_string2" ) );
		assert rev3.getStrings().equals( TestTools.makeList( "sle1_string3", "sle1_string2" ) );
	}

	@Test
	public void testHistoryOfSse2() {
		StringListEntity rev1 = getAuditReader().find( StringListEntity.class, sle2_id, 1 );
		StringListEntity rev2 = getAuditReader().find( StringListEntity.class, sle2_id, 2 );
		StringListEntity rev3 = getAuditReader().find( StringListEntity.class, sle2_id, 3 );

		assert rev1.getStrings().equals( TestTools.makeList( "sle2_string1", "sle2_string2" ) );
		assert rev2.getStrings().equals( TestTools.makeList( "sle2_string1", "sle2_string2", "sle2_string1" ) );
		assert rev3.getStrings().equals( TestTools.makeList( "sle2_string2", "sle2_string1" ) );
	}
}