/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

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
		final Long id = scope.fromTransaction( session -> {
			final UserEntity user = new UserEntity( "test_user" );
			session.persist( user );
			final GoodCompany company = new GoodCompany();
			company.employee = user;
			session.persist( company );
			final CompanyRegistry companyRegistry = new CompanyRegistry( List.of( company ) );
			session.persist( companyRegistry );
			return companyRegistry.id;
		} );
		scope.inSession( session -> {
			final CompanyRegistry companyRegistry = session.get( CompanyRegistry.class, id );
			assertThat( companyRegistry.getCompanies() ).hasSize( 1 )
					.extracting( AbstractCompany::getEmployee )
					.extracting( UserEntity::getName )
					.containsOnly( "test_user" );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from CompanyRegistry" ).executeUpdate();
			session.createMutationQuery( "delete from AbstractCompany" ).executeUpdate();
			session.createMutationQuery( "delete from SuperEntity" ).executeUpdate();
		} );
	}

	@Entity( name = "SuperEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	static abstract class SuperEntity {
		@Id
		@GeneratedValue
		Long id;
	}

	@Entity( name = "CompanyRegistry" )
	static class CompanyRegistry extends SuperEntity {
		@ManyToMany( fetch = FetchType.LAZY )
		List<AbstractCompany> companies;

		public CompanyRegistry() {
		}

		public CompanyRegistry(List<AbstractCompany> companies) {
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

		public UserEntity getEmployee() {
			return employee;
		}
	}

	@Entity( name = "GoodCompany" )
	static class GoodCompany extends AbstractCompany {
	}

	@Entity( name = "BadCompany" )
	static class BadCompany extends AbstractCompany {
		// (unused) sibling subtype for 'GoodCompany'
	}

	@Entity( name = "AbstractUser" )
	static class BaseUser extends SuperEntity {
		// necessary intermediate entity so 'BaseUser' is the first child type and gets the lowest discriminator value
	}

	@Entity( name = "UserEntity" )
	static class UserEntity extends BaseUser {
		private String name;

		public UserEntity() {
		}

		public UserEntity(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}


