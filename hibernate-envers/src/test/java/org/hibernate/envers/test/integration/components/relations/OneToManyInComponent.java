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
import org.hibernate.envers.test.entities.components.relations.OneToManyComponent;
import org.hibernate.envers.test.entities.components.relations.OneToManyComponentTestEntity;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( message = "Collection role is incorrect when contained in an @Embeddable" )
public class OneToManyInComponent extends BaseEnversJPAFunctionalTestCase {
	private Integer otmcte_id1;
	private Integer ste_id1;
	private Integer ste_id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {OneToManyComponentTestEntity.class, StrTestEntity.class, OneToManyComponent.class};
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

		OneToManyComponentTestEntity otmcte1 = new OneToManyComponentTestEntity( new OneToManyComponent( "data1" ) );
		otmcte1.getComp1().getEntities().add( ste1 );

		em.persist( otmcte1 );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		otmcte1 = em.find( OneToManyComponentTestEntity.class, otmcte1.getId() );
		otmcte1.getComp1().getEntities().add( ste2 );

		em.getTransaction().commit();

		otmcte_id1 = otmcte1.getId();
		ste_id1 = ste1.getId();
		ste_id2 = ste2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 2, 3 ).equals(
				getAuditReader().getRevisions(
						OneToManyComponentTestEntity.class,
						otmcte_id1
				)
		);
	}

	@Test
	public void testHistoryOfId1() {
		StrTestEntity ste1 = getEntityManager().find( StrTestEntity.class, ste_id1 );
		StrTestEntity ste2 = getEntityManager().find( StrTestEntity.class, ste_id2 );

		OneToManyComponentTestEntity ver2 = new OneToManyComponentTestEntity(
				otmcte_id1, new OneToManyComponent(
				"data1"
		)
		);
		ver2.getComp1().getEntities().add( ste1 );
		OneToManyComponentTestEntity ver3 = new OneToManyComponentTestEntity(
				otmcte_id1, new OneToManyComponent(
				"data1"
		)
		);
		ver3.getComp1().getEntities().add( ste1 );
		ver3.getComp1().getEntities().add( ste2 );

		assert getAuditReader().find( OneToManyComponentTestEntity.class, otmcte_id1, 1 ) == null;
		assert getAuditReader().find( OneToManyComponentTestEntity.class, otmcte_id1, 2 ).equals( ver2 );
		assert getAuditReader().find( OneToManyComponentTestEntity.class, otmcte_id1, 3 ).equals( ver3 );
	}
}