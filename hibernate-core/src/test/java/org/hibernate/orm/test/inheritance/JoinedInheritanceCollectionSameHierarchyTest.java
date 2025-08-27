/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		// The user entities come first, so they get the lowest discriminator values
		JoinedInheritanceCollectionSameHierarchyTest.UserEntity.class,
		JoinedInheritanceCollectionSameHierarchyTest.BaseUser.class,
		JoinedInheritanceCollectionSameHierarchyTest.SuperEntity.class,
		JoinedInheritanceCollectionSameHierarchyTest.AbstractCompany.class,
		JoinedInheritanceCollectionSameHierarchyTest.GoodCompany.class,
		JoinedInheritanceCollectionSameHierarchyTest.BadCompany.class,
		JoinedInheritanceCollectionSameHierarchyTest.CompanyRegistry.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17161" )
public class JoinedInheritanceCollectionSameHierarchyTest {
	@Test
	public void testGetDiscriminatorCollection(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final CompanyRegistry companyRegistry = session.find( CompanyRegistry.class, 30 );
			assertThat( companyRegistry.getCompanies() ).hasSize( 1 )
					.extracting( AbstractCompany::getEmployee )
					.extracting( UserEntity::getName )
					.containsOnly( "test_user" );
		} );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( session -> {
			final UserEntity user = new UserEntity( 1, "test_user" );
			session.persist( user );
			final GoodCompany company = new GoodCompany( 20 );
			company.employee = user;
			session.persist( company );
			final CompanyRegistry companyRegistry = new CompanyRegistry( 30, List.of( company ) );
			session.persist( companyRegistry );
		} );
	}

	@AfterAll
	public void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

	@Entity( name = "SuperEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	static abstract class SuperEntity {
		@Id
		Integer id;

		public SuperEntity() {
		}

		public SuperEntity(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "CompanyRegistry" )
	static class CompanyRegistry extends SuperEntity {
		@ManyToMany( fetch = FetchType.LAZY )
		List<AbstractCompany> companies;

		public CompanyRegistry() {
		}

		public CompanyRegistry(Integer id, List<AbstractCompany> companies) {
			super( id );
			this.companies = companies;
		}

		public List<AbstractCompany> getCompanies() {
			return companies;
		}
	}

	@Entity( name = "AbstractCompany" )
	abstract static class AbstractCompany extends SuperEntity {
		@ManyToOne( fetch = FetchType.LAZY )
		UserEntity employee;

		public AbstractCompany() {
		}

		public AbstractCompany(Integer id) {
			super( id );
		}

		public UserEntity getEmployee() {
			return employee;
		}
	}

	@Entity( name = "GoodCompany" )
	static class GoodCompany extends AbstractCompany {
		public GoodCompany() {
		}

		public GoodCompany(Integer id) {
			super( id );
		}
	}

	@Entity( name = "BadCompany" )
	static class BadCompany extends AbstractCompany {
		// (unused) sibling subtype for 'GoodCompany'

		public BadCompany() {
		}

		public BadCompany(Integer id) {
			super( id );
		}
	}

	@Entity( name = "AbstractUser" )
	static class BaseUser extends SuperEntity {
		// necessary intermediate entity so 'BaseUser' is the first child type and gets the lowest discriminator value

		public BaseUser() {
		}

		public BaseUser(Integer id) {
			super( id );
		}
	}

	@Entity( name = "UserEntity" )
	static class UserEntity extends BaseUser {
		private String name;

		public UserEntity() {
		}

		public UserEntity(Integer id, String name) {
			super( id );
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
