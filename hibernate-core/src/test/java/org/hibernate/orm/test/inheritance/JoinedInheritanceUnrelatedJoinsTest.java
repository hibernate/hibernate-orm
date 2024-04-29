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

@DomainModel(annotatedClasses = {
		// The user entities come first, so they get the lowest discriminator values
		JoinedInheritanceUnrelatedJoinsTest.UserEntity.class,
		JoinedInheritanceUnrelatedJoinsTest.BaseUser.class,
		JoinedInheritanceUnrelatedJoinsTest.SuperEntity.class,
		JoinedInheritanceUnrelatedJoinsTest.AbstractCompany.class,
		JoinedInheritanceUnrelatedJoinsTest.GoodCompany.class,
		JoinedInheritanceUnrelatedJoinsTest.BadCompany.class,
		JoinedInheritanceUnrelatedJoinsTest.CompanyRegistry.class,
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-17161")
public class JoinedInheritanceUnrelatedJoinsTest {

	@Test
	public void testGetDiscriminatorCollection(SessionFactoryScope scope) {
		Long id = scope.fromTransaction( session -> {
			UserEntity user = new UserEntity();
			session.persist( user );
			GoodCompany company = new GoodCompany();
			company.employee = user;
			session.persist( company );
			CompanyRegistry companyRegistry = new CompanyRegistry( List.of( company ) );
			session.persist( companyRegistry );
			return companyRegistry.id;
		} );
		scope.inTransaction( session -> {
			CompanyRegistry companyRegistry = session.get( CompanyRegistry.class, id );
			assertThat( companyRegistry.companies ).hasSize( 1 );
		} );
	}

	@Entity(name = "SuperEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class SuperEntity {
		@Id
		@GeneratedValue
		Long id;
	}

	@Entity(name = "CompanyRegistry")
	public static class CompanyRegistry extends SuperEntity {

		@ManyToMany(fetch = FetchType.LAZY)
		private List<AbstractCompany> companies;

		public CompanyRegistry(List<AbstractCompany> companies) {
			this.companies = companies;
		}

		public CompanyRegistry() {
		}
	}

	@Entity(name = "AbstractCompany")
	public abstract static class AbstractCompany extends SuperEntity {
		@ManyToOne(fetch = FetchType.LAZY)
		UserEntity employee;
	}

	@Entity(name = "GoodCompany")
	public static class GoodCompany extends AbstractCompany {
	}

	@Entity(name = "BadCompany")
	public static class BadCompany extends AbstractCompany {
		// (unused) sibling subtype for 'GoodCompany'
	}

	@Entity(name = "AbstractUser")
	public static class BaseUser extends SuperEntity {
		// necessary intermediate entity so 'BaseUser' is the first child type and gets the lowest discriminator value
	}

	@Entity(name = "UserEntity")
	public static class UserEntity extends BaseUser {
	}
}


