/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.FetchProfile;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;

/**
 * @author Jan Schatteman
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		InheritedTypeAssociationJoinedFetchTest.Animal.class,
		InheritedTypeAssociationJoinedFetchTest.Tiger.class,
		InheritedTypeAssociationJoinedFetchTest.Elephant.class,
		InheritedTypeAssociationJoinedFetchTest.Zoo.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17328" )
public class InheritedTypeAssociationJoinedFetchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Zoo zoo = new Zoo( 1L );
			session.persist( zoo );
			session.persist( new Tiger( "tiger", zoo ) );
			session.persist( new Elephant( "elephant", zoo ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Animal" ).executeUpdate();
			session.createMutationQuery( "delete from Zoo" ).executeUpdate();
		} );
	}

	@Test
	public void testExplicitJoinFetch(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final Zoo zoo = session.createQuery(
					"from Zoo z join fetch z.tiger join fetch z.elephants",
					Zoo.class
			).getSingleResult();
			assertResult( zoo, inspector );
		} );
	}

	@Test
	public void testEntityGraph(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final EntityGraph<?> entityGraph = session.getEntityGraph( "graph-with-all-animals" );
			final Zoo zoo = session.createQuery( "from Zoo", Zoo.class )
					.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph )
					.getSingleResult();
			assertResult( zoo, inspector );
		} );
	}

	@Test
	public void testFetchProfile(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			session.enableFetchProfile( "profile-with-all-animals" );
			final Zoo zoo = session.createQuery( "from Zoo", Zoo.class )
					.getSingleResult();
			assertResult( zoo, inspector );
		} );
	}

	private void assertResult(final Zoo zoo, final SQLStatementInspector inspector) {
		assertThat( Hibernate.isInitialized( zoo.getTiger() ) ).isTrue();
		assertThat( zoo.getTiger().getName() ).isEqualTo( "tiger" );
		assertThat( Hibernate.isInitialized( zoo.getElephants() ) ).isTrue();
		assertThat( zoo.getElephants() ).hasSize( 1 );
		assertThat( zoo.getElephants().get( 0 ).getName() ).isEqualTo( "elephant" );

		inspector.assertExecutedCount( 1 );
		inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "animal_type", 2 );
	}

	@Entity( name = "Animal" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "animal_type" )
	public static class Animal {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn( name = "zoo_id" )
		private Zoo zoo;

		public Animal() {
		}

		public Animal(String name, Zoo zoo) {
			this.name = name;
			this.zoo = zoo;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "Tiger" )
	@DiscriminatorValue( "Tiger" )
	public static class Tiger extends Animal {
		public Tiger() {
		}

		public Tiger(String name, Zoo zoo) {
			super( name, zoo );
		}
	}

	@Entity( name = "Elephant" )
	@DiscriminatorValue( "Elephant" )
	public static class Elephant extends Animal {
		public Elephant() {
		}

		public Elephant(String name, Zoo zoo) {
			super( name, zoo );
		}
	}

	@Entity( name = "Zoo" )
	@NamedEntityGraph( name = "graph-with-all-animals", attributeNodes = {
			@NamedAttributeNode( value = "tiger" ),
			@NamedAttributeNode( value = "elephants" )
	} )
	@FetchProfile( name = "profile-with-all-animals", fetchOverrides = {
			@FetchProfile.FetchOverride( association = "tiger", entity = Zoo.class, fetch = FetchType.EAGER ),
			@FetchProfile.FetchOverride( association = "elephants", entity = Zoo.class, fetch = FetchType.EAGER ),
	} )
	public static class Zoo {
		@Id
		private Long id;

		@OneToOne( mappedBy = "zoo", fetch = FetchType.LAZY )
		private Tiger tiger;

		@OneToMany( mappedBy = "zoo", fetch = FetchType.LAZY )
		private List<Elephant> elephants;

		public Zoo() {
		}

		public Zoo(Long id) {
			this.id = id;
		}

		public Tiger getTiger() {
			return tiger;
		}

		public List<Elephant> getElephants() {
			return elephants;
		}
	}
}
