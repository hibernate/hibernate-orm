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
import java.util.List;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.EnumSetEntity;
import org.hibernate.envers.test.entities.collection.EnumSetEntity.E1;
import org.hibernate.envers.test.entities.collection.EnumSetEntity.E2;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EnumSet extends BaseEnversJPAFunctionalTestCase {
	private Integer sse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EnumSetEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EnumSetEntity sse1 = new EnumSetEntity();

		// Revision 1 (sse1: initialy 1 element)
		em.getTransaction().begin();

		sse1.getEnums1().add( E1.X );
		sse1.getEnums2().add( E2.A );

		em.persist( sse1 );

		em.getTransaction().commit();

		// Revision 2 (sse1: adding 1 element/removing a non-existing element)
		em.getTransaction().begin();

		sse1 = em.find( EnumSetEntity.class, sse1.getId() );

		sse1.getEnums1().add( E1.Y );
		sse1.getEnums2().remove( E2.B );

		em.getTransaction().commit();

		// Revision 3 (sse1: removing 1 element/adding an exisiting element)
		em.getTransaction().begin();

		sse1 = em.find( EnumSetEntity.class, sse1.getId() );

		sse1.getEnums1().remove( E1.X );
		sse1.getEnums2().add( E2.A );

		em.getTransaction().commit();

		//

		sse1_id = sse1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( EnumSetEntity.class, sse1_id ) );
	}

	@Test
	public void testHistoryOfSse1() {
		EnumSetEntity rev1 = getAuditReader().find( EnumSetEntity.class, sse1_id, 1 );
		EnumSetEntity rev2 = getAuditReader().find( EnumSetEntity.class, sse1_id, 2 );
		EnumSetEntity rev3 = getAuditReader().find( EnumSetEntity.class, sse1_id, 3 );

		assert rev1.getEnums1().equals( TestTools.makeSet( E1.X ) );
		assert rev2.getEnums1().equals( TestTools.makeSet( E1.X, E1.Y ) );
		assert rev3.getEnums1().equals( TestTools.makeSet( E1.Y ) );

		assert rev1.getEnums2().equals( TestTools.makeSet( E2.A ) );
		assert rev2.getEnums2().equals( TestTools.makeSet( E2.A ) );
		assert rev3.getEnums2().equals( TestTools.makeSet( E2.A ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7780")
	@FailureExpectedWithNewMetamodel( message = "Enum attributes represented by String not supported yet." )
	public void testEnumRepresentation() {
		EntityManager entityManager = getEntityManager();
		List<Object> enums1 = entityManager.createNativeQuery(
				"SELECT enums1 FROM EnumSetEntity_enums1_AUD ORDER BY rev ASC"
		).getResultList();
		List<Object> enums2 = entityManager.createNativeQuery(
				"SELECT enums2 FROM EnumSetEntity_enums2_AUD ORDER BY rev ASC"
		).getResultList();
		entityManager.close();

		Assert.assertEquals( Arrays.asList( "X", "Y", "X" ), enums1 );

		Assert.assertEquals( 1, enums2.size() );
		Object enum2 = enums2.get( 0 );
		// Compare the Strings to account for, as an example, Oracle returning a BigDecimal instead of an int.
		Assert.assertEquals( "0", enum2.toString() );
	}
}