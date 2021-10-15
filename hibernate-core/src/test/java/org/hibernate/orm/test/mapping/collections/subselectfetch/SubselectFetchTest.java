/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.subselectfetch;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
@SessionFactory
@NotImplementedYet( strict = false, reason = "Need to check why these fail" )
public class SubselectFetchTest {
	@Test
	public void testSubselectFetchHql(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Parent p = new Parent( "foo" );
					p.getChildren().add( new Child( "foo1" ) );
					p.getChildren().add( new Child( "foo2" ) );
					Parent q = new Parent( "bar" );
					q.getChildren().add( new Child( "bar1" ) );
					q.getChildren().add( new Child( "bar2" ) );
					q.getMoreChildren().addAll( p.getChildren() );
					s.persist( p );
					s.persist( q );
				}
		);

		scope.inTransaction(
				s -> {
					scope.getSessionFactory().getStatistics().clear();

					List parents = s.createQuery( "from Parent where name between 'bar' and 'foo' order by name desc" )
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

					s.delete( p );
					s.delete( q );
				}
		);
	}

	@Test
	public void testSubselectFetchNamedParam(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Parent p = new Parent( "foo" );
					p.getChildren().add( new Child( "foo1" ) );
					p.getChildren().add( new Child( "foo2" ) );
					Parent q = new Parent( "bar" );
					q.getChildren().add( new Child( "bar1" ) );
					q.getChildren().add( new Child( "bar2" ) );
					q.getMoreChildren().addAll( p.getChildren() );
					s.persist( p );
					s.persist( q );
				}
		);

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

					s.delete( p );
					s.delete( q );
				}
		);
	}

	@Test
	public void testSubselectFetchPosParam(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Parent p = new Parent( "foo" );
					p.getChildren().add( new Child( "foo1" ) );
					p.getChildren().add( new Child( "foo2" ) );
					Parent q = new Parent( "bar" );
					q.getChildren().add( new Child( "bar1" ) );
					q.getChildren().add( new Child( "bar2" ) );
					q.getMoreChildren().addAll( p.getChildren() );
					s.persist( p );
					s.persist( q );
				}
		);

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

					Child c = (Child) p.getChildren().get( 0 );
					c.getFriends().size();

					s.delete( p );
					s.delete( q );
				}
		);
	}

	@Test
	public void testSubselectFetchWithLimit(SessionFactoryScope scope) {

		Parent parent = new Parent( "foo" );
		Parent secondParent = new Parent( "bar" );
		Parent thirdParent = new Parent( "aaa" );

		scope.inTransaction(
				s -> {
					parent.getChildren().add( new Child( "foo1" ) );
					parent.getChildren().add( new Child( "foo2" ) );
					secondParent.getChildren().add( new Child( "bar1" ) );
					secondParent.getChildren().add( new Child( "bar2" ) );
					thirdParent.getChildren().add( new Child( "aaa1" ) );
					s.persist( parent );
					s.persist( secondParent );
					s.persist( thirdParent );
				}
		);

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

					s.delete( p );
					s.delete( q );
					s.delete( r );
				}
		);
	}

	@Test
	public void testManyToManyCriteriaJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Parent p = new Parent( "foo" );
					p.getChildren().add( new Child( "foo1" ) );
					p.getChildren().add( new Child( "foo2" ) );
					Parent q = new Parent( "bar" );
					q.getChildren().add( new Child( "bar1" ) );
					q.getChildren().add( new Child( "bar2" ) );
					q.getMoreChildren().addAll( p.getChildren() );
					s.persist( p );
					s.persist( q );
				}
		);

		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Parent> criteria = criteriaBuilder.createQuery( Parent.class );
					Root<Parent> root = criteria.from( Parent.class );
					root.join( "moreChildren", JoinType.INNER )
							.join( "friends", JoinType.INNER );
					criteria.orderBy( criteriaBuilder.desc( root.get( "name" ) ) );

					s.createQuery( criteria ).list();
//					List parents = s.createCriteria( Parent.class )
//							.createCriteria( "moreChildren" )
//							.createCriteria( "friends" )
//							.addOrder( Order.desc( "name" ) )
//							.list();


					criteria = criteriaBuilder.createQuery( Parent.class );
					root = criteria.from( Parent.class );
					root.fetch( "moreChildren", JoinType.LEFT ).fetch( "friends", JoinType.LEFT );
					criteria.orderBy( criteriaBuilder.desc( root.get( "name" ) ) );

					List parents = s.createQuery( criteria ).list();

//					parents = s.createCriteria( Parent.class )
//							.setFetchMode( "moreChildren", FetchMode.JOIN )
//							.setFetchMode( "moreChildren.friends", FetchMode.JOIN )
//							.addOrder( Order.desc( "name" ) )
//							.list();

					s.delete( parents.get( 0 ) );
					s.delete( parents.get( 1 ) );

				}
		);
	}

	@Test
	public void testSubselectFetchCriteria(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Parent p = new Parent( "foo" );
					p.getChildren().add( new Child( "foo1" ) );
					p.getChildren().add( new Child( "foo2" ) );
					Parent q = new Parent( "bar" );
					q.getChildren().add( new Child( "bar1" ) );
					q.getChildren().add( new Child( "bar2" ) );
					q.getMoreChildren().addAll( p.getChildren() );
					s.persist( p );
					s.persist( q );
				}
		);


		scope.inTransaction(
				s -> {
					scope.getSessionFactory().getStatistics().clear();

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Parent> criteria = criteriaBuilder.createQuery( Parent.class );
					Root<Parent> root = criteria.from( Parent.class );
					criteria.where( criteriaBuilder.between( root.get( "name" ), "bar", "foo" ) );
					criteria.orderBy( criteriaBuilder.desc( root.get( "name" ) ) );

					List parents = s.createQuery( criteria ).list();
//					List parents = s.createCriteria( Parent.class )
//							.add( Property.forName( "name" ).between( "bar", "foo" ) )
//							.addOrder( Order.desc( "name" ) )
//							.list();
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

					s.delete( p );
					s.delete( q );
				}
		);

	}

}

