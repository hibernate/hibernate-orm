/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.spi.PersistentCollection;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" )
		}
)
@DomainModel(
		xmlMappings = "/mappings/subselectfetch/ParentChild.hbm.xml"
)
@SessionFactory( useCollectingStatementInspector = true )
public class SubselectFetchTest {
	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Parent foo = new Parent("foo");
			foo.getChildren().add( new Child("foo1") );
			foo.getChildren().add( new Child("foo2") );

			Parent bar = new Parent("bar");
			bar.getChildren().add( new Child("bar1") );
			bar.getChildren().add( new Child("bar2") );
			bar.getMoreChildren().addAll( foo.getChildren() );

			session.persist(foo);
			session.persist(bar);
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.remove( session.getReference( Parent.class, "foo" ) );
			session.remove( session.getReference( Parent.class, "bar" ) );
		} );
	}

	@Test
	public void simplifiedTestSubSelectFetchHql(SessionFactoryScope scope) {
		scope.inTransaction( (s) -> {
			scope.getSessionFactory().getStatistics().clear();

			final List<Parent> parents = s
					.createQuery("from Parent where name between 'bar' and 'foo' order by name desc", Parent.class )
					.list();
			final Parent foo = parents.get( 0 );
			final Parent bar = parents.get( 1 );

			assertThat( scope.getSessionFactory().getStatistics().getPrepareStatementCount() ).isEqualTo( 1 );
			assertThat( Hibernate.isInitialized( foo.getChildren() ) ).isFalse();
			assertThat( Hibernate.isInitialized( bar.getChildren() ) ).isFalse();

			Hibernate.initialize( foo.getChildren() );

			assertThat( scope.getSessionFactory().getStatistics().getPrepareStatementCount() ).isEqualTo( 2 );
			assertThat( Hibernate.isInitialized( foo.getChildren() ) ).isTrue();
			assertThat( Hibernate.isInitialized( bar.getChildren() ) ).isTrue();

			assertThat( ( (PersistentCollection) foo.getChildren() ).getOwner() ).isEqualTo( foo );
		} );
	}

	@Test
	public void testSubselectFetchHql(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				s -> {
					scope.getSessionFactory().getStatistics().clear();

					final List<Parent> parents = s
							.createQuery( "from Parent where name between 'bar' and 'foo' order by name desc", Parent.class )
							.list();
					final Parent foo = parents.get( 0 );
					final Parent bar = parents.get( 1 );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					statementInspector.clear();

					assertThat( Hibernate.isInitialized( foo.getChildren() ) ).isFalse();
					assertThat( Hibernate.isInitialized( bar.getChildren() ) ).isFalse();

					// triggers initialization as side effect
					assertThat( foo.getChildren() ).hasSize( 2 );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					statementInspector.clear();

					assertThat( Hibernate.isInitialized( foo.getChildren() ) ).isTrue();
					assertThat( Hibernate.isInitialized( bar.getChildren() ) ).isTrue();

					// access bar's children and make sure it triggers no SQL
					assertThat( bar.getChildren() ).hasSize( 2 );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 0 );
					statementInspector.clear();

					assertThat( Hibernate.isInitialized( foo.getChildren().iterator().next() ) ).isTrue();
					assertThat( Hibernate.isInitialized( bar.getChildren().iterator().next() ) ).isTrue();

					assertThat( Hibernate.isInitialized( foo.getMoreChildren() ) ).isFalse();
					assertThat( Hibernate.isInitialized( bar.getMoreChildren() ) ).isFalse();

					assertThat( foo.getMoreChildren() ).hasSize( 0 );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

					assertThat( Hibernate.isInitialized( foo.getMoreChildren() ) ).isTrue();
					assertThat( Hibernate.isInitialized( bar.getMoreChildren() ) ).isTrue();
				}
		);
	}

	@Test
	public void testSubselectFetchNamedParam(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					scope.getSessionFactory().getStatistics().clear();

					List parents = s.createQuery( "from Parent where name between :bar and :foo order by name desc" )
							.setParameter( "bar", "bar" )
							.setParameter( "foo", "foo" )
							.list();
					Parent p = (Parent) parents.get( 0 );
					Parent q = (Parent) parents.get( 1 );

					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertFalse( Hibernate.isInitialized( q.getChildren() ) );

					assertEquals( p.getChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( p.getChildren().iterator().next() ) );

					assertTrue( Hibernate.isInitialized( q.getChildren() ) );

					assertEquals( q.getChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( q.getChildren().iterator().next() ) );

					assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
					assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

					assertEquals( p.getMoreChildren().size(), 0 );

					assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );

					assertEquals( q.getMoreChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( q.getMoreChildren().iterator().next() ) );

					assertEquals( 3, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );

					Child c = (Child) p.getChildren().get( 0 );
					c.getFriends().size();
				}
		);
	}

	@Test
	public void testSubselectFetchPosParam(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					scope.getSessionFactory().getStatistics().clear();

					List parents = s.createQuery( "from Parent where name between ?1 and ?2 order by name desc" )
							.setParameter( 1, "bar" )
							.setParameter( 2, "foo" )
							.list();
					Parent p = (Parent) parents.get( 0 );
					Parent q = (Parent) parents.get( 1 );

					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertFalse( Hibernate.isInitialized( q.getChildren() ) );

					assertEquals( p.getChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( p.getChildren().iterator().next() ) );

					assertTrue( Hibernate.isInitialized( q.getChildren() ) );

					assertEquals( q.getChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( q.getChildren().iterator().next() ) );

					assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
					assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

					assertEquals( p.getMoreChildren().size(), 0 );

					assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );

					assertEquals( q.getMoreChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( q.getMoreChildren().iterator().next() ) );

					assertEquals( 3, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testSubselectFetchWithLimit(SessionFactoryScope scope) {
		Parent thirdParent = new Parent( "aaa" );
		thirdParent.getChildren().add( new Child( "aaa1" ) );

		scope.inTransaction(
				s -> {
					s.persist( thirdParent );
				}
		);

		try {
			scope.inTransaction(
					s -> {
						scope.getSessionFactory().getStatistics().clear();

						List parents = s.createQuery( "from Parent order by name desc" )
								.setMaxResults( 2 )
								.list();
						Parent p = (Parent) parents.get( 0 );
						Parent q = (Parent) parents.get( 1 );
						assertFalse( Hibernate.isInitialized( p.getChildren() ) );
						assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
						assertFalse( Hibernate.isInitialized( q.getChildren() ) );
						assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );
						assertEquals( p.getMoreChildren().size(), 0 );
						assertEquals( p.getChildren().size(), 2 );
						assertTrue( Hibernate.isInitialized( q.getChildren() ) );
						assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );

						assertEquals( 3, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );

						Parent r = s.get( Parent.class, thirdParent.getName() );
						assertTrue( Hibernate.isInitialized( r.getChildren() ) );
						assertFalse( Hibernate.isInitialized( r.getMoreChildren() ) );
						assertEquals( r.getChildren().size(), 1 );
						assertEquals( r.getMoreChildren().size(), 0 );
					}
			);
		}
		finally {
			scope.inTransaction( (session) -> {
				session.remove( session.getReference( Parent.class, "aaa" ) );
			} );
		}
	}

	@Test
	public void testManyToManyCriteriaJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Parent> criteria = criteriaBuilder.createQuery( Parent.class );
					Root<Parent> root = criteria.from( Parent.class );
					root.join( "moreChildren", JoinType.INNER )
							.join( "friends", JoinType.INNER );
					criteria.orderBy( criteriaBuilder.desc( root.get( "name" ) ) );

					s.createQuery( criteria ).list();

					criteria = criteriaBuilder.createQuery( Parent.class );
					root = criteria.from( Parent.class );
					root.fetch( "moreChildren", JoinType.LEFT ).fetch( "friends", JoinType.LEFT );
					criteria.orderBy( criteriaBuilder.desc( root.get( "name" ) ) );
				}
		);
	}

	@Test
	public void testSubselectFetchCriteria(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				s -> {
					scope.getSessionFactory().getStatistics().clear();

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Parent> criteria = criteriaBuilder.createQuery( Parent.class );
					Root<Parent> root = criteria.from( Parent.class );
					criteria.where( criteriaBuilder.between( root.get( "name" ), "bar", "foo" ) );
					criteria.orderBy( criteriaBuilder.desc( root.get( "name" ) ) );

					List parents = s.createQuery( criteria ).list();
					Parent p = (Parent) parents.get( 0 );
					Parent q = (Parent) parents.get( 1 );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					assertThat( Hibernate.isInitialized( p.getChildren() ) ).isFalse();
					assertThat( Hibernate.isInitialized( q.getChildren() ) ).isFalse();

					statementInspector.clear();
					Hibernate.initialize( p.getChildren() );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					assertThat( Hibernate.isInitialized( p.getChildren() ) ).isTrue();
					assertThat( Hibernate.isInitialized( q.getChildren() ) ).isTrue();

					assertEquals( p.getChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( p.getChildren().iterator().next() ) );

					assertTrue( Hibernate.isInitialized( q.getChildren() ) );

					assertEquals( q.getChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( q.getChildren().iterator().next() ) );

					assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
					assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

					assertEquals( p.getMoreChildren().size(), 0 );

					assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );

					assertEquals( q.getMoreChildren().size(), 2 );

					assertTrue( Hibernate.isInitialized( q.getMoreChildren().iterator().next() ) );

					assertEquals( 3, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );
				}
		);

	}

}

