/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.relations;

import java.util.Arrays;
import javax.persistence.EntityManager;

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
		return new Class[] {ManyToOneComponentTestEntity.class, StrTestEntity.class};

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
