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
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponent;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponentTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ManyToOneInComponent extends BaseEnversJPAFunctionalTestCase {
	private Integer mtocte_id1;
	private Integer ste_id1;
	private Integer ste_id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ManyToOneComponentTestEntity.class, StrTestEntity.class, ManyToOneComponent.class};

	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrTestEntity ste1 = new StrTestEntity();
		ste1.setStr( "str1" );

		StrTestEntity ste2 = new StrTestEntity();
		ste2.setStr( "str2" );

		em.persist( ste1 );
		em.persist( ste2 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		ManyToOneComponentTestEntity mtocte1 = new ManyToOneComponentTestEntity(
				new ManyToOneComponent(
						ste1,
						"data1"
				)
		);

		em.persist( mtocte1 );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		mtocte1 = em.find( ManyToOneComponentTestEntity.class, mtocte1.getId() );
		mtocte1.getComp1().setEntity( ste2 );

		em.getTransaction().commit();

		mtocte_id1 = mtocte1.getId();
		ste_id1 = ste1.getId();
		ste_id2 = ste2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 2, 3 ).equals(
				getAuditReader().getRevisions(
						ManyToOneComponentTestEntity.class,
						mtocte_id1
				)
		);
	}

	@Test
	public void testHistoryOfId1() {
		StrTestEntity ste1 = getEntityManager().find( StrTestEntity.class, ste_id1 );
		StrTestEntity ste2 = getEntityManager().find( StrTestEntity.class, ste_id2 );

		ManyToOneComponentTestEntity ver2 = new ManyToOneComponentTestEntity(
				mtocte_id1, new ManyToOneComponent(
				ste1,
				"data1"
		)
		);
		ManyToOneComponentTestEntity ver3 = new ManyToOneComponentTestEntity(
				mtocte_id1, new ManyToOneComponent(
				ste2,
				"data1"
		)
		);

		assert getAuditReader().find( ManyToOneComponentTestEntity.class, mtocte_id1, 1 ) == null;
		assert getAuditReader().find( ManyToOneComponentTestEntity.class, mtocte_id1, 2 ).equals( ver2 );
		assert getAuditReader().find( ManyToOneComponentTestEntity.class, mtocte_id1, 3 ).equals( ver3 );
	}
}
