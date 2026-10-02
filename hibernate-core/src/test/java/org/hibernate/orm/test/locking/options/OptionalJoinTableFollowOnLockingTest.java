package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.LockMode;
import org.hibernate.Locking;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		OptionalJoinTableFollowOnLockingTest.Organization.class,
		OptionalJoinTableFollowOnLockingTest.OrganizationGroup.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20822")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
@Tag("db-locking")
class OptionalJoinTableFollowOnLockingTest {
	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var group = new OrganizationGroup();
			group.id = 1L;
			session.persist( group );
			session.persist( new Organization( 1L, null ) );
			session.persist( new Organization( 2L, group ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testMissingAssociation(SessionFactoryScope scope) {
		assertLockedOrganization( scope, 1L, null );
	}

	@Test
	void testPresentAssociation(SessionFactoryScope scope) {
		assertLockedOrganization( scope, 2L, 1L );
	}

	@Test
	void testMixedAssociations(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var organizations = session.createQuery(
						"from Organization order by id", Organization.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.getResultList();

			assertThat( organizations ).hasSize( 2 );
			assertThat( organizations.get( 0 ).group ).isNull();
			assertThat( organizations.get( 1 ).group.id ).isEqualTo( 1L );
			assertThat( organizations ).allSatisfy( organization ->
					assertThat( session.getCurrentLockMode( organization ) )
							.isEqualTo( LockMode.PESSIMISTIC_WRITE ) );
		} );
	}

	private void assertLockedOrganization(SessionFactoryScope scope, Long id, Long groupId) {
		scope.inTransaction( session -> {
			final var organization = session.createQuery(
						"from Organization where id = :id", Organization.class )
					.setParameter( "id", id )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.getSingleResult();

			assertThat( organization.id ).isEqualTo( id );
			assertThat( session.getCurrentLockMode( organization ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			if ( groupId == null ) {
				assertThat( organization.group ).isNull();
			}
			else {
				assertThat( organization.group.id ).isEqualTo( groupId );
			}
		} );
	}

	@Entity(name = "Organization")
	@Table(name = "organization")
	static class Organization {
		@Id
		private Long id;

		@ManyToOne
		@JoinTable(
				name = "organization_group_organization",
				joinColumns = @JoinColumn(name = "organization_id"),
				inverseJoinColumns = @JoinColumn(name = "group_id")
		)
		private OrganizationGroup group;

		Organization() {
		}

		Organization(Long id, OrganizationGroup group) {
			this.id = id;
			this.group = group;
		}
	}

	@Entity(name = "OrganizationGroup")
	@Table(name = "organization_group")
	static class OrganizationGroup {
		@Id
		private Long id;
	}
}
