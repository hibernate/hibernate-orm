/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the handling and expansion of list parameters,
 * particularly when using {@code @Fetch(FetchMode.SUBSELECT)}
 * (because this fetch mode involves building a map of parameters).
 */
@DomainModel(
		annotatedClasses = {
				QueryListParametersWithFetchSubSelectTest.Parent.class,
				QueryListParametersWithFetchSubSelectTest.Child.class
		}
)
@SessionFactory(
		useCollectingStatementInspector = true
)
public class QueryListParametersWithFetchSubSelectTest {

	@BeforeAll
	protected void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			for ( int i = 0; i < 10; i++ ) {
				Parent parent = new Parent( i );
				s.persist( parent );
				for ( int j = 0; j < 10; j++ ) {
					Child child = new Child( i * 100 + j, parent );
					parent.children.add( child );
					s.persist( child );
				}
			}
		} );
	}

	@Test
	public void simpleTest(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();

		scope.inTransaction( s -> {
			TypedQuery<Parent> query = s.createQuery( "select p from Parent p where id in :ids", Parent.class );
			query.setParameter( "ids", Arrays.asList( 0, 1, 2 ) );
			List<Parent> results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactlyInAnyOrder( 0, 1, 2 );
		} );

		// If we get here, children were initialized eagerly.
		// Did ORM actually use subselects?
		assertThat( sqlStatementInterceptor.getSqlQueries() ).hasSize( 2 );
	}

	@Test
	@JiraKey(value = "HHH-14439")
	public void reusingQueryWithFewerNamedParameters(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();

		sqlStatementInterceptor.clear();

		scope.inTransaction( s -> {
			TypedQuery<Parent> query = s.createQuery( "select p from Parent p where id in :ids", Parent.class );

			query.setParameter( "ids", Arrays.asList( 0, 1, 2, 3 ) );
			List<Parent> results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactlyInAnyOrder( 0, 1, 2, 3 );

			query.setParameter( "ids", Arrays.asList( 4, 5, 6 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactlyInAnyOrder( 4, 5, 6 );

			query.setParameter( "ids", Arrays.asList( 7, 8 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactlyInAnyOrder( 7, 8 );
		} );

		// If we get here, children were initialized eagerly.
		// Did ORM actually use subselects?
		assertThat( sqlStatementInterceptor.getSqlQueries() ).hasSize( 3 * 2 );
	}


	@Test
	@JiraKey(value = "HHH-14439")
	public void reusingQueryWithFewerOrdinalParameters(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();

		sqlStatementInterceptor.clear();

		scope.inTransaction( s -> {
			TypedQuery<Parent> query = s.createQuery( "select p from Parent p where id in ?1", Parent.class );

			query.setParameter( 1, Arrays.asList( 0, 1, 2, 3 ) );
			List<Parent> results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactlyInAnyOrder( 0, 1, 2, 3 );

			query.setParameter( 1, Arrays.asList( 4, 5, 6 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactlyInAnyOrder( 4, 5, 6 );

			query.setParameter( 1, Arrays.asList( 7, 8 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactlyInAnyOrder( 7, 8 );
		} );

		// If we get here, children were initialized eagerly.
		// Did ORM actually use subselects?
		assertThat( sqlStatementInterceptor.getSqlQueries() ).hasSize( 3 * 2 );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;

		@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Integer id;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

}
