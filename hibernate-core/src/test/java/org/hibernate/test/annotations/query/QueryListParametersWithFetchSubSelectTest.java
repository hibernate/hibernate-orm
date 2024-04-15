/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.annotations.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.TypedQuery;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the handling and expansion of list parameters,
 * particularly when using {@code @Fetch(FetchMode.SUBSELECT)}
 * (because this fetch mode involves building a map of parameters).
 */
public class QueryListParametersWithFetchSubSelectTest extends BaseCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configure(Configuration configuration) {
		sqlStatementInterceptor = new SQLStatementInterceptor( configuration );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class };
	}

	@Override
	protected void afterSessionFactoryBuilt() {
		inTransaction( s -> {
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
	public void simple() {
		sqlStatementInterceptor.clear();

		inTransaction( s -> {
			TypedQuery<Parent> query = s.createQuery( "select p from Parent p where id in :ids", Parent.class );
			query.setParameter( "ids", Arrays.asList( 0, 1, 2 ) );
			List<Parent> results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactly( 0, 1, 2 );
		} );

		// If we get here, children were initialized eagerly.
		// Did ORM actually use subselects?
		assertThat( sqlStatementInterceptor.getSqlQueries() ).hasSize( 2 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14439")
	public void reusingQueryWithFewerNamedParameters() {
		sqlStatementInterceptor.clear();

		inTransaction( s -> {
			TypedQuery<Parent> query = s.createQuery( "select p from Parent p where id in :ids", Parent.class );

			query.setParameter( "ids", Arrays.asList( 0, 1, 2, 3 ) );
			List<Parent> results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactly( 0, 1, 2, 3 );

			query.setParameter( "ids", Arrays.asList( 4, 5, 6 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactly( 4, 5, 6 );

			query.setParameter( "ids", Arrays.asList( 7, 8 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactly( 7, 8 );
		} );

		// If we get here, children were initialized eagerly.
		// Did ORM actually use subselects?
		assertThat( sqlStatementInterceptor.getSqlQueries() ).hasSize( 3 * 2 );
	}


	@Test
	@TestForIssue(jiraKey = "HHH-14439")
	public void reusingQueryWithFewerOrdinalParameters() {
		sqlStatementInterceptor.clear();

		inTransaction( s -> {
			TypedQuery<Parent> query = s.createQuery( "select p from Parent p where id in ?0", Parent.class );

			query.setParameter( 0, Arrays.asList( 0, 1, 2, 3 ) );
			List<Parent> results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactly( 0, 1, 2, 3 );

			query.setParameter( 0, Arrays.asList( 4, 5, 6 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactly( 4, 5, 6 );

			query.setParameter( 0, Arrays.asList( 7, 8 ) );
			results = query.getResultList();
			assertThat( results )
					.allSatisfy( parent -> assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isTrue() )
					.extracting( Parent::getId ).containsExactly( 7, 8 );
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
