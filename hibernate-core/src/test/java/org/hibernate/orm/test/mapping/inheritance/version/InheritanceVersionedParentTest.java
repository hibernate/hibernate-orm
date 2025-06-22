/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.version;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		InheritanceVersionedParentTest.VersionedFruit.class,
		InheritanceVersionedParentTest.Raspberry.class,
		InheritanceVersionedParentTest.Drupelet.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16667" )
public class InheritanceVersionedParentTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Drupelet drupelet = new Drupelet();
			session.persist( drupelet );
			final Raspberry raspberry = new Raspberry( 1L, "green" );
			raspberry.getDrupelets().add( drupelet );
			session.persist( raspberry );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "from Raspberry", Raspberry.class )
				.getResultList()
				.forEach( r -> {
					r.getDrupelets().clear();
					session.remove( r );
				} ) );
	}

	@Test
	public void testUpdateBasic(SessionFactoryScope scope) {
		final Long prev = scope.fromTransaction( session -> {
			final Raspberry raspberry = session.find( Raspberry.class, 1L );
			raspberry.setMaturity( "ripe" );
			return raspberry.getVersion();
		} );
		scope.inTransaction(
				session -> assertThat( session.find( Raspberry.class, 1L ).getVersion() ).isEqualTo( prev + 1 )
		);
	}

	@Test
	public void testUpdateAssociation(SessionFactoryScope scope) {
		final Long prev = scope.fromTransaction( session -> {
			final Raspberry raspberry = session.find( Raspberry.class, 1L );
			raspberry.getDrupelets().clear();
			return raspberry.getVersion();
		} );
		scope.inTransaction(
				session -> assertThat( session.find( Raspberry.class, 1L ).getVersion() ).isEqualTo( prev + 1 )
		);
	}

	@Entity( name = "VersionedFruit" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class VersionedFruit {
		@Id
		private Long id;

		@Version
		private Long version;

		public VersionedFruit() {
		}

		public VersionedFruit(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Long getVersion() {
			return version;
		}
	}

	@Entity( name = "Raspberry" )
	@PrimaryKeyJoinColumn( name = "raspberry_id" )
	public static class Raspberry extends VersionedFruit {
		@OneToMany
		private Set<Drupelet> drupelets = new HashSet<>();

		private String maturity;

		public Raspberry() {
		}

		public Raspberry(Long id, String maturity) {
			super( id );
			this.maturity = maturity;
		}

		public Set<Drupelet> getDrupelets() {
			return drupelets;
		}

		public String getMaturity() {
			return maturity;
		}

		public void setMaturity(String name) {
			this.maturity = name;
		}
	}

	@Entity( name = "Drupelet" )
	public static class Drupelet {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}
}
