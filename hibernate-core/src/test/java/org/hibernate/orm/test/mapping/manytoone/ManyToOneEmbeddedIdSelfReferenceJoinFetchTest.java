/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Staffan Hörke
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		ManyToOneEmbeddedIdSelfReferenceJoinFetchTest.OrganizationId.class,
		ManyToOneEmbeddedIdSelfReferenceJoinFetchTest.OrganizationEmbedded.class,
		ManyToOneEmbeddedIdSelfReferenceJoinFetchTest.Organization.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16473" )
public class ManyToOneEmbeddedIdSelfReferenceJoinFetchTest {
	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final OrganizationEmbedded parent = new OrganizationEmbedded(
					new OrganizationId( 1L ),
					"parent_organization"
			);
			final OrganizationEmbedded child = new OrganizationEmbedded(
					new OrganizationId( 2L ),
					"child_organization"
			);
			parent.getChildren().add( child );
			child.setParent( parent );
			session.persist( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			final Organization parent = new Organization( 1L, "parent_organization" );
			final Organization child = new Organization( 2L, "child_organization" );
			parent.getChildren().add( child );
			child.setParent( parent );
			session.persist( parent );
			session.persist( child );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "from OrganizationEmbedded", OrganizationEmbedded.class )
					.getResultList()
					.forEach( o -> o.setParent( null ) );
			session.createQuery( "from Organization", Organization.class )
					.getResultList()
					.forEach( o -> o.setParent( null ) );
		} );
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from OrganizationEmbedded" ).executeUpdate();
			session.createMutationQuery( "delete from Organization" ).executeUpdate();
		} );
	}

	@Test
	public void testChildWithEmbeddedId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final OrganizationEmbedded child = session.createQuery(
					"select child from OrganizationEmbedded child join fetch child.parent WHERE child.id = :id",
					OrganizationEmbedded.class
			).setParameter( "id", new OrganizationId( 2L ) ).getSingleResult();
			assertThat( child.getName() ).isEqualTo( "child_organization" );
			assertThat( child.getParent() ).isNotNull();
			assertThat( child.getParent().getName() ).isEqualTo( "parent_organization" );
		} );
	}

	@Test
	public void testChildWithPrimitiveId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Organization child = session.createQuery(
					"select child from Organization child join fetch child.parent where child.id = :id",
					Organization.class
			).setParameter( "id", 2L ).getSingleResult();
			assertThat( child.getName() ).isEqualTo( "child_organization" );
			assertThat( child.getParent() ).isNotNull();
			assertThat( child.getParent().getName() ).isEqualTo( "parent_organization" );
		} );
	}

	@Embeddable
	public static class OrganizationId implements Serializable {
		private Long id;

		public OrganizationId() {
		}

		public OrganizationId(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "OrganizationEmbedded" )
	public static class OrganizationEmbedded {
		@EmbeddedId
		private OrganizationId id;
		private String name;
		@ManyToOne
		private OrganizationEmbedded parent;
		@OneToMany( mappedBy = "parent" )
		private Set<OrganizationEmbedded> children;

		public OrganizationEmbedded() {
		}

		public OrganizationEmbedded(OrganizationId id, String name) {
			this.id = id;
			this.name = name;
			this.children = new HashSet<>();
		}

		public String getName() {
			return name;
		}

		public OrganizationEmbedded getParent() {
			return parent;
		}

		public void setParent(OrganizationEmbedded parent) {
			this.parent = parent;
		}

		public Set<OrganizationEmbedded> getChildren() {
			return children;
		}
	}

	@Entity( name = "Organization" )
	public static class Organization {
		@Id
		private Long id;
		private String name;
		@ManyToOne
		private Organization parent;
		@OneToMany( mappedBy = "parent" )
		private Set<Organization> children;

		public Organization() {
		}

		public Organization(Long id, String name) {
			this.id = id;
			this.name = name;
			this.children = new HashSet<>();
		}

		public String getName() {
			return name;
		}

		public Organization getParent() {
			return parent;
		}

		public void setParent(Organization parent) {
			this.parent = parent;
		}

		public Set<Organization> getChildren() {
			return children;
		}
	}
}
