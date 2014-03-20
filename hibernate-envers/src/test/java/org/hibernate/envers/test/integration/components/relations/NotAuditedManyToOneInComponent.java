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
package org.hibernate.envers.test.integration.components.relations;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.components.relations.NotAuditedManyToOneComponent;
import org.hibernate.envers.test.entities.components.relations.NotAuditedManyToOneComponentTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotAuditedManyToOneInComponent extends BaseEnversJPAFunctionalTestCase {
	private Integer mtocte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				NotAuditedManyToOneComponentTestEntity.class,
				UnversionedStrTestEntity.class,
				NotAuditedManyToOneComponent.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		// No revision
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		UnversionedStrTestEntity ste1 = new UnversionedStrTestEntity();
		ste1.setStr( "str1" );

		UnversionedStrTestEntity ste2 = new UnversionedStrTestEntity();
		ste2.setStr( "str2" );

		em.persist( ste1 );
		em.persist( ste2 );

		em.getTransaction().commit();

		// Revision 1
		em = getEntityManager();
		em.getTransaction().begin();

		NotAuditedManyToOneComponentTestEntity mtocte1 = new NotAuditedManyToOneComponentTestEntity(
				new NotAuditedManyToOneComponent( ste1, "data1" )
		);

		em.persist( mtocte1 );

		em.getTransaction().commit();

		// No revision
		em = getEntityManager();
		em.getTransaction().begin();

		mtocte1 = em.find( NotAuditedManyToOneComponentTestEntity.class, mtocte1.getId() );
		mtocte1.getComp1().setEntity( ste2 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		mtocte1 = em.find( NotAuditedManyToOneComponentTestEntity.class, mtocte1.getId() );
		mtocte1.getComp1().setData( "data2" );

		em.getTransaction().commit();

		mtocte_id1 = mtocte1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 )
				.equals( getAuditReader().getRevisions( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		NotAuditedManyToOneComponentTestEntity ver1 = new NotAuditedManyToOneComponentTestEntity(
				mtocte_id1,
				new NotAuditedManyToOneComponent( null, "data1" )
		);
		NotAuditedManyToOneComponentTestEntity ver2 = new NotAuditedManyToOneComponentTestEntity(
				mtocte_id1,
				new NotAuditedManyToOneComponent( null, "data2" )
		);

		assert getAuditReader().find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 2 ).equals( ver2 );
	}
}