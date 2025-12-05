/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.relation;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-3843")
@Jpa(
		annotatedClasses = {
				Person.class, Role.class, RightsSubject.class
		}
)
@EnversTest
public class ParentReferencingChildTest {
	Person expLukaszRev1 = null;
	Role expAdminRev1 = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		Person lukasz = new Person();
		Role admin = new Role();
		scope.inTransaction(
				entityManager -> {
					// Revision 1
					lukasz.setName( "Lukasz" );
					lukasz.setGroup( "IT" );
					entityManager.persist( lukasz );
					admin.setName( "Admin" );
					admin.setGroup( "Confidential" );
					lukasz.getRoles().add( admin );
					admin.getMembers().add( lukasz );
					entityManager.persist( admin );
				}
		);

		expLukaszRev1 = new Person( lukasz.getId(), "IT", "Lukasz" );
		expAdminRev1 = new Role( admin.getId(), "Confidential", "Admin" );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					assertThat( AuditReaderFactory.get( entityManager )
							.getRevisions( Person.class, expLukaszRev1.getId() ) )
							.isEqualTo( Arrays.asList( 1 ) );
					assertThat( AuditReaderFactory.get( entityManager )
							.getRevisions( RightsSubject.class, expLukaszRev1.getId() ) )
							.isEqualTo( Arrays.asList( 1 ) );

					assertThat( AuditReaderFactory.get( entityManager )
							.getRevisions( Role.class, expAdminRev1.getId() ) )
							.isEqualTo( Arrays.asList( 1 ) );
					assertThat( AuditReaderFactory.get( entityManager ).getRevisions(
							RightsSubject.class,
							expAdminRev1.getId()
					) ).isEqualTo( Arrays.asList( 1 ) );
				}
		);
	}

	@Test
	public void testHistoryOfLukasz(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Person lukaszRev1 = AuditReaderFactory.get( entityManager )
							.find( Person.class, expLukaszRev1.getId(), 1 );
					RightsSubject rightsSubjectLukaszRev1 = AuditReaderFactory.get( entityManager )
							.find( RightsSubject.class, expLukaszRev1.getId(), 1 );

					assertThat( lukaszRev1 ).isEqualTo( expLukaszRev1 );
					assertThat( lukaszRev1.getRoles() ).isEqualTo( TestTools.makeSet( expAdminRev1 ) );
					assertThat( rightsSubjectLukaszRev1.getRoles() ).isEqualTo( TestTools.makeSet( expAdminRev1 ) );
				}
		);
	}

	@Test
	public void testHistoryOfAdmin(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Role adminRev1 = AuditReaderFactory.get( entityManager )
							.find( Role.class, expAdminRev1.getId(), 1 );

					assertThat( adminRev1 ).isEqualTo( expAdminRev1 );
					assertThat( adminRev1.getMembers() ).isEqualTo( TestTools.makeSet( expLukaszRev1 ) );
				}
		);
	}
}
