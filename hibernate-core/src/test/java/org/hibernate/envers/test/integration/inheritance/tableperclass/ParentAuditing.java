/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.inheritance.tableperclass;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParentAuditing extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildEntity.class, ParentEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		id1 = 1;

		// Rev 1
		em.getTransaction().begin();
		ParentEntity pe = new ParentEntity( id1, "x" );
		em.persist( pe );
		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();
		pe = em.find( ParentEntity.class, id1 );
		pe.setData( "y" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ParentEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfChildId1() {
		assert getAuditReader().find( ChildEntity.class, id1, 1 ) == null;
		assert getAuditReader().find( ChildEntity.class, id1, 2 ) == null;
	}

	@Test
	public void testHistoryOfParentId1() {
		ParentEntity ver1 = new ParentEntity( id1, "x" );
		ParentEntity ver2 = new ParentEntity( id1, "y" );

		assert getAuditReader().find( ParentEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ParentEntity.class, id1, 2 ).equals( ver2 );
	}

	@Test
	public void testPolymorphicQuery() {
		ParentEntity parentVer1 = new ParentEntity( id1, "x" );

		assert getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult()
				.equals( parentVer1 );
		assert getAuditReader().createQuery().forEntitiesAtRevision( ChildEntity.class, 1 )
				.getResultList().size() == 0;
	}
}