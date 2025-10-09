/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.Hibernate;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		ManyToManyJoinTableAndInheritanceTest.RootEntity.class,
		ManyToManyJoinTableAndInheritanceTest.ParentEntity.class,
		ManyToManyJoinTableAndInheritanceTest.Sub1.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17679" )
public class ManyToManyJoinTableAndInheritanceTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Sub1 sub1 = new Sub1( 1L, 1, 1 );
			session.persist( sub1 );
			final RootEntity root = new RootEntity();
			root.getNodesPoly().add( sub1 );
			session.persist( root );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testLeftJoinSelectParent(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			//noinspection removal
			final RootEntity result = session.createQuery(
					"select root from RootEntity root left join root.nodesPoly _collection",
					RootEntity.class
			).getSingleResult();
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
			assertThat( Hibernate.isInitialized( result.getNodesPoly() ) ).isFalse();
			assertThat( result.getNodesPoly() ).hasSize( 1 );
			assertThat( result.getNodesPoly().iterator().next().getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testLeftJoinSelectId(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			//noinspection removal
			final Long result = session.createQuery(
					"select _collection.id from RootEntity root left join root.nodesPoly _collection",
					Long.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1L );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
		} );
	}

	@Test
	public void testLeftJoinSelectElement(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			//noinspection removal
			final Sub1 result = session.createQuery(
					"select _collection from RootEntity root left join root.nodesPoly _collection",
					Sub1.class
			).getSingleResult();
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
			assertThat( result.getId() ).isEqualTo( 1L );
			assertThat( result.getNumber() ).isEqualTo( 1 );
			assertThat( result.getSub1Value() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testLeftJoinFetch(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			//noinspection removal
			final RootEntity result = session.createQuery(
					"select root from RootEntity root left join fetch root.nodesPoly _collection",
					RootEntity.class
			).getSingleResult();
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
			assertThat( Hibernate.isInitialized( result.getNodesPoly() ) ).isTrue();
			assertThat( result.getNodesPoly() ).hasSize( 1 );
			assertThat( result.getNodesPoly().iterator().next().getId() ).isEqualTo( 1L );
		} );
	}

	@SuppressWarnings({"unused", "FieldMayBeFinal"})
	@Entity( name = "RootEntity" )
	public static class RootEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany
		@JoinTable(
				name = "set_one_to_many_poly",
				joinColumns = @JoinColumn( name = "root_id" ),
				inverseJoinColumns = @JoinColumn( name = "poly_id" )
		)
		private Set<Sub1> nodesPoly = new HashSet<>();

		public Set<Sub1> getNodesPoly() {
			return nodesPoly;
		}
	}

	@Entity( name = "ParentEntity" )
	@DiscriminatorValue( "0" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class ParentEntity {
		@Id
		private Long id;

		@Column( name = "number_col" )
		private Integer number;

		public ParentEntity() {
		}

		public ParentEntity(Long id, Integer number) {
			this.id = id;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public Integer getNumber() {
			return number;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "Sub1" )
	@DiscriminatorValue( "1" )
	public static class Sub1 extends ParentEntity {
		private Integer sub1Value;

		public Sub1() {
		}

		public Sub1(Long id, Integer number, Integer sub1Value) {
			super( id, number );
			this.sub1Value = sub1Value;
		}

		public Integer getSub1Value() {
			return sub1Value;
		}
	}
}
