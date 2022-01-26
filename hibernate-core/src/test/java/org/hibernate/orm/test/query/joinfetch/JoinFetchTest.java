/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.joinfetch;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.MAX_FETCH_DEPTH;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = MAX_FETCH_DEPTH, value = "10" ),
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "false" )
		}
)
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/query/joinfetch/ItemBid.hbm.xml",
				"org/hibernate/orm/test/query/joinfetch/UserGroup.hbm.xml"
		}
)
@SessionFactory
public class JoinFetchTest {

//	@Test
//	public void testProjection() {
//		inTransaction(
//				s -> {
//					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
//					CriteriaQuery<Long> criteria = criteriaBuilder.createQuery( Long.class );
//					criteria.select( criteriaBuilder.count( criteria.from( Item.class ) ) );
//					s.createQuery( criteria ).uniqueResult();
//
//					CriteriaQuery<Item> itemCriteria = criteriaBuilder.createQuery( Item.class );
//					itemCriteria.from( Item.class );
//					s.createQuery( itemCriteria ).uniqueResult();
//
////					s.createCriteria(Item.class).setProjection( Projections.rowCount() ).uniqueResult();
////					s.createCriteria(Item.class).uniqueResult();
//				}
//		);
//	}

	@Test
	@NotImplementedYet( strict = false )
	public void testJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( (s) -> {
			s.createQuery( "delete from Bid" ).executeUpdate();
			s.createQuery( "delete from Comment" ).executeUpdate();
			s.createQuery( "delete from Item" ).executeUpdate();
		} );

		Category cat = new Category( "Photography" );
		Item i = new Item( cat, "Camera" );
		Bid b = new Bid( i, 100.0f );
		new Bid( i, 105.0f );
		new Comment( i, "This looks like a really good deal" );
		new Comment( i, "Is it the latest version?" );
		new Comment( i, "<comment deleted>" );

		scope.inTransaction( (s) -> {
			s.persist( cat );
			s.persist( i );
		} );

		scope.getSessionFactory().getCache().evictEntityData( Item.class );


		scope.inTransaction( (s) -> {
			Item i1 = s.get( Item.class, i.getId() );
			assertTrue( Hibernate.isInitialized( i1.getBids() ) );
			assertEquals( i1.getBids().size(), 2 );
			assertTrue( Hibernate.isInitialized( i1.getComments() ) );
			assertEquals( i1.getComments().size(), 3 );
		} );


		scope.getSessionFactory().getCache().evictEntityData( Bid.class );

		scope.inTransaction( (s) -> {
			Bid b1 = s.get( Bid.class, b.getId() );
			assertTrue( Hibernate.isInitialized( b1.getItem() ) );
			assertTrue( Hibernate.isInitialized( b1.getItem().getComments() ) );
			assertEquals( b1.getItem().getComments().size(), 3 );
		} );

		scope.getSessionFactory().getCache().evictCollectionData( Item.class.getName() + ".bids" );

		scope.inTransaction( (s) -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Item> criteria = criteriaBuilder.createQuery( Item.class );
			Root<Item> root = criteria.from( Item.class );
			root.join( "bids" );
			root.join( "comments" );

			Item i1 = s.createQuery( criteria ).uniqueResult();
//					Item i1 = (Item) s.createCriteria( Item.class )from
//							.setFetchMode( "bids", FetchMode.SELECT )
//							.setFetchMode( "comments", FetchMode.SELECT )
//							.uniqueResult();
			assertFalse( Hibernate.isInitialized( i1.getBids() ) );
			assertFalse( Hibernate.isInitialized( i1.getComments() ) );
			Bid b1 = (Bid) i1.getBids().iterator().next();
			assertTrue( Hibernate.isInitialized( b1.getItem() ) );
		} );


		scope.inTransaction( (s) -> {
			Item i1 = (Item) s.createQuery( "from Item i left join fetch i.bids left join fetch i.comments" ).uniqueResult();
			assertTrue( Hibernate.isInitialized( i1.getBids() ) );
			assertTrue( Hibernate.isInitialized( i1.getComments() ) );
			assertEquals( i1.getComments().size(), 3 );
			assertEquals( i1.getBids().size(), 2 );
		} );


		scope.inTransaction( (s) -> {
			Item i1 = (Item) ((Object[])s.getNamedQuery( Item.class.getName() + ".all" ).list().get( 0 ))[0];
			assertTrue( Hibernate.isInitialized( i1.getBids() ) );
			assertTrue( Hibernate.isInitialized( i1.getComments() ) );
				assertEquals( i1.getComments().size(), 3 );
				assertEquals( i1.getBids().size(), 2 );
		} );

		scope.inTransaction( (s) -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Item> criteria = criteriaBuilder.createQuery( Item.class );
			criteria.from( Item.class );
			Item i1 = s.createQuery( criteria ).uniqueResult();
			assertFalse( Hibernate.isInitialized( i1.getBids() ) );
			assertFalse( Hibernate.isInitialized( i1.getComments() ) );
			assertEquals( i1.getComments().size(), 3 );
			assertEquals( i1.getBids().size(), 2 );
		} );

		scope.inTransaction( (s) -> {
			List bids = s.createQuery( "select b from Bid b left join fetch b.item i left join fetch i.category" ).list();
			Bid bid = (Bid) bids.get( 0 );
			assertTrue( Hibernate.isInitialized( bid.getItem() ) );
			assertTrue( Hibernate.isInitialized( bid.getItem().getCategory() ) );
		} );

		scope.inTransaction( (s) -> {
			List pairs = s.createQuery( "select i from Item i left join i.bids b left join fetch i.category" ).list();
			Item item = (Item) pairs.get( 0 );
			assertFalse( Hibernate.isInitialized( item.getBids() ) );
			assertTrue( Hibernate.isInitialized( item.getCategory() ) );
			s.clear();
			pairs = s.createQuery( "select i, b from Item i left join i.bids b left join i.category" ).list();
			item = (Item) ((Object[])pairs.get( 0 ))[0];
			assertFalse( Hibernate.isInitialized( item.getBids() ) );
			assertFalse( Hibernate.isInitialized( item.getCategory() ) );
			s.clear();
			pairs = s.createQuery( "select i from Item i left join i.bids b left join i.category" ).list();
			item = (Item) pairs.get( 0 );
			assertFalse( Hibernate.isInitialized( item.getBids() ) );
			assertFalse( Hibernate.isInitialized( item.getCategory() ) );
			s.clear();
			pairs = s.createQuery( "select b, i from Bid b left join b.item i left join fetch i.category" ).list();
			Bid bid = (Bid) ( (Object[]) pairs.get( 0 ) )[0];
			assertTrue( Hibernate.isInitialized( bid.getItem() ) );
			assertTrue( Hibernate.isInitialized( bid.getItem().getCategory() ) );
			s.clear();
			pairs = s.createQuery( "select b, i from Bid b left join b.item i left join i.category" ).list();
			bid = (Bid) ( (Object[]) pairs.get( 0 ) )[0];
			assertTrue( Hibernate.isInitialized( bid.getItem() ) );
			assertFalse( Hibernate.isInitialized( bid.getItem().getCategory() ) );
			pairs = s.createQuery( "select b from Bid b left join b.item i left join i.category" ).list();
			bid = (Bid)  pairs.get( 0 ) ;
			assertTrue( Hibernate.isInitialized( bid.getItem() ) );
			assertFalse( Hibernate.isInitialized( bid.getItem().getCategory() ) );
		} );

		scope.inTransaction( (s) -> {
			s.createQuery( "delete from Bid" ).executeUpdate();
			s.createQuery( "delete from Comment" ).executeUpdate();
			s.createQuery( "delete from Item" ).executeUpdate();
			s.createQuery( "delete from Category" ).executeUpdate();
		} );
	}

//	@Test
//	public void testCollectionFilter() {
//		inTransaction(
//				s -> {
//					Group hb = new Group( "hibernate" );
//					User gavin = new User( "gavin" );
//					User max = new User( "max" );
//					hb.getUsers().put( "gavin", gavin );
//					hb.getUsers().put( "max", max );
//					gavin.getGroups().put( "hibernate", hb );
//					max.getGroups().put( "hibernate", hb );
//					s.persist( hb );
//				}
//		);
//
//		inTransaction(
//				s -> {
//					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
//					CriteriaQuery<Group> criteria = criteriaBuilder.createQuery( Group.class );
//					Root<Group> from = criteria.from( Group.class );
////					from.join( "users", JoinType.LEFT );
//					criteria.where( criteriaBuilder.equal( from.get( "name" ), "hibernate" ) );
//					Group hb = s.createQuery( criteria ).uniqueResult();
////		hb = (Group) s.createCriteria( Group.class )
////				.setFetchMode( "users", FetchMode.SELECT )
////				.add( Restrictions.idEq( "hibernate" ) )
////				.uniqueResult();
////					assertFalse( Hibernate.isInitialized( hb.getUsers() ) );
////					gavin = (User) s.createFilter( hb.getUsers(), "where index(this) = 'gavin'" ).uniqueResult();
////		Long size = (Long) s.createFilter( hb.getUsers(), "select count(*)" ).uniqueResult();
////		assertEquals( new Long( 2 ), size );
////		assertFalse( Hibernate.isInitialized( hb.getUsers() ) );
//					s.delete( hb );
//				}
//		);
//	}

	@Test
	@NotImplementedYet( strict = false )
	public void testJoinFetchManyToMany(SessionFactoryScope scope) {
		Group group = new Group( "hibernate" );

		scope.inTransaction( (s) -> {
			User gavin = new User( "gavin" );
			User max = new User( "max" );
			group.getUsers().put( "gavin", gavin );
			group.getUsers().put( "max", max );
			gavin.getGroups().put( "hibernate", group );
			max.getGroups().put( "hibernate", group );
			s.persist( group );
		} );

		scope.inTransaction( (s) -> {
			Group hb = s.get( Group.class, "hibernate" );
			assertTrue( Hibernate.isInitialized( hb.getUsers() ) );
			User gavin = (User) hb.getUsers().get( "gavin" );
			assertFalse( Hibernate.isInitialized( gavin.getGroups() ) );
			User max = s.get( User.class, "max" );
			assertFalse( Hibernate.isInitialized( max.getGroups() ) );
		} );

		scope.inTransaction( (s) -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Group> criteria = criteriaBuilder.createQuery( Group.class );
			Root<Group> from = criteria.from( Group.class );
			from.fetch( "users", JoinType.LEFT ).fetch( "groups" );
			Group hb = s.createQuery( criteria ).uniqueResult();
//		hb = (Group) s.createCriteria( Group.class )
//				.setFetchMode( "users", FetchMode.JOIN )
//				.setFetchMode( "users.groups", FetchMode.JOIN )
//				.uniqueResult();
			assertTrue( Hibernate.isInitialized( hb.getUsers() ) );
			User gavin = (User) hb.getUsers().get( "gavin" );
			assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
			User max = s.get( User.class, "max" );
			assertTrue( Hibernate.isInitialized( max.getGroups() ) );
		} );


		scope.inTransaction( (s) -> s.delete( group ) );
	}

}
