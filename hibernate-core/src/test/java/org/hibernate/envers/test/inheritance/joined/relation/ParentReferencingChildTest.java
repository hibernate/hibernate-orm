/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.joined.relation;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.Person;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.RightsSubject;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.Role;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-3843")
@Disabled("NYI - Joined Inheritance Support")
public class ParentReferencingChildTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	Person expLukaszRev1 = null;
	Role expAdminRev1 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Role.class, RightsSubject.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Person lukasz = new Person();
					lukasz.setName( "Lukasz" );
					lukasz.setGroup( "IT" );
					entityManager.persist( lukasz );

					Role admin = new Role();
					admin.setName( "Admin" );
					admin.setGroup( "Confidential" );
					lukasz.getRoles().add( admin );
					admin.getMembers().add( lukasz );
					entityManager.persist( admin );

					expLukaszRev1 = new Person( lukasz.getId(), "IT", "Lukasz" );
					expAdminRev1 = new Role( admin.getId(), "Confidential", "Admin" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( Person.class, expLukaszRev1.getId() ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( RightsSubject.class, expLukaszRev1.getId() ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( Role.class, expAdminRev1.getId() ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( RightsSubject.class, expAdminRev1.getId() ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfLukasz() {
		Person lukaszRev1 = getAuditReader().find( Person.class, expLukaszRev1.getId(), 1 );
		RightsSubject rightsSubjectLukaszRev1 = getAuditReader().find( RightsSubject.class, expLukaszRev1.getId(), 1 );

		assertThat( lukaszRev1, equalTo( expLukaszRev1 ) );
		assertThat( lukaszRev1.getRoles(), contains( expAdminRev1 ) );
		assertThat( rightsSubjectLukaszRev1.getRoles(), contains( expAdminRev1 ) );
	}

	@DynamicTest
	public void testHistoryOfAdmin() {
		Role adminRev1 = getAuditReader().find( Role.class, expAdminRev1.getId(), 1 );

		assertThat( adminRev1, equalTo( expAdminRev1 ) );
		assertThat( adminRev1.getMembers(), contains( expLukaszRev1 ) );
	}
}
