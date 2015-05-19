/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.inheritance.tableperclass.childrelation;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChildReferencing extends BaseEnversJPAFunctionalTestCase {
	private Integer re_id1;
	private Integer re_id2;
	private Integer c_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildIngEntity.class, ParentNotIngEntity.class, ReferencedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		re_id1 = 1;
		re_id2 = 10;
		c_id = 100;

		// Rev 1
		em.getTransaction().begin();

		ReferencedEntity re1 = new ReferencedEntity( re_id1 );
		em.persist( re1 );

		ReferencedEntity re2 = new ReferencedEntity( re_id2 );
		em.persist( re2 );

		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();

		re1 = em.find( ReferencedEntity.class, re_id1 );

		ChildIngEntity cie = new ChildIngEntity( c_id, "y", 1l );
		cie.setReferenced( re1 );
		em.persist( cie );
		c_id = cie.getId();

		em.getTransaction().commit();

		// Rev 3
		em.getTransaction().begin();

		re2 = em.find( ReferencedEntity.class, re_id2 );
		cie = em.find( ChildIngEntity.class, c_id );

		cie.setReferenced( re2 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( ReferencedEntity.class, re_id1 ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( ReferencedEntity.class, re_id2 ) );
		assert Arrays.asList( 2, 3 ).equals( getAuditReader().getRevisions( ChildIngEntity.class, c_id ) );
	}

	@Test
	public void testHistoryOfReferencedCollection1() {
		assert getAuditReader().find( ReferencedEntity.class, re_id1, 1 ).getReferencing().size() == 0;
		assert getAuditReader().find( ReferencedEntity.class, re_id1, 2 ).getReferencing().equals(
				TestTools.makeSet( new ChildIngEntity( c_id, "y", 1l ) )
		);
		assert getAuditReader().find( ReferencedEntity.class, re_id1, 3 ).getReferencing().size() == 0;
	}

	@Test
	public void testHistoryOfReferencedCollection2() {
		assert getAuditReader().find( ReferencedEntity.class, re_id2, 1 ).getReferencing().size() == 0;
		assert getAuditReader().find( ReferencedEntity.class, re_id2, 2 ).getReferencing().size() == 0;
		assert getAuditReader().find( ReferencedEntity.class, re_id2, 3 ).getReferencing().equals(
				TestTools.makeSet( new ChildIngEntity( c_id, "y", 1l ) )
		);
	}

	@Test
	public void testChildHistory() {
		assert getAuditReader().find( ChildIngEntity.class, c_id, 1 ) == null;
		assert getAuditReader().find( ChildIngEntity.class, c_id, 2 ).getReferenced().equals(
				new ReferencedEntity( re_id1 )
		);
		assert getAuditReader().find( ChildIngEntity.class, c_id, 3 ).getReferenced().equals(
				new ReferencedEntity( re_id2 )
		);
	}
}