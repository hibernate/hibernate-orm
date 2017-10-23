/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.inheritance.joined.relation;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-3843")
public class ParentReferencingChildTest extends BaseEnversJPAFunctionalTestCase {
	Person expLukaszRev1 = null;
	Role expAdminRev1 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Role.class, RightsSubject.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		Person lukasz = new Person();
		lukasz.setName( "Lukasz" );
		lukasz.setGroup( "IT" );
		em.persist( lukasz );
		Role admin = new Role();
		admin.setName( "Admin" );
		admin.setGroup( "Confidential" );
		lukasz.getRoles().add( admin );
		admin.getMembers().add( lukasz );
		em.persist( admin );
		em.getTransaction().commit();

		expLukaszRev1 = new Person( lukasz.getId(), "IT", "Lukasz" );
		expAdminRev1 = new Role( admin.getId(), "Confidential", "Admin" );
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Person.class, expLukaszRev1.getId() ) );
		Assert.assertEquals(
				Arrays.asList( 1 ), getAuditReader().getRevisions(
				RightsSubject.class,
				expLukaszRev1.getId()
		)
		);

		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Role.class, expAdminRev1.getId() ) );
		Assert.assertEquals(
				Arrays.asList( 1 ), getAuditReader().getRevisions(
				RightsSubject.class,
				expAdminRev1.getId()
		)
		);
	}

	@Test
	public void testHistoryOfLukasz() {
		Person lukaszRev1 = getAuditReader().find( Person.class, expLukaszRev1.getId(), 1 );
		RightsSubject rightsSubjectLukaszRev1 = getAuditReader().find( RightsSubject.class, expLukaszRev1.getId(), 1 );

		Assert.assertEquals( expLukaszRev1, lukaszRev1 );
		Assert.assertEquals( TestTools.makeSet( expAdminRev1 ), lukaszRev1.getRoles() );
		Assert.assertEquals( TestTools.makeSet( expAdminRev1 ), rightsSubjectLukaszRev1.getRoles() );
	}

	@Test
	public void testHistoryOfAdmin() {
		Role adminRev1 = getAuditReader().find( Role.class, expAdminRev1.getId(), 1 );

		Assert.assertEquals( expAdminRev1, adminRev1 );
		Assert.assertEquals( TestTools.makeSet( expLukaszRev1 ), adminRev1.getMembers() );
	}
}
