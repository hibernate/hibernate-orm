/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.referencedcolumnname;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				House.class,
				Postman.class,
				Bag.class,
				Rambler.class,
				Luggage.class,
				Clothes.class,
				Inhabitant.class,
				Item.class,
				ItemCost.class,
				Vendor.class,
				WarehouseItem.class,
				Place.class,
				HousePlaces.class
		}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
				provider = ReferencedColumnNameTest.NamingStrategyProvider.class
		)
)
public class ReferencedColumnNameTest {
	public static class NamingStrategyProvider implements SettingProvider.Provider<ImplicitNamingStrategy> {
		@Override
		public ImplicitNamingStrategy getSetting() {
			return ImplicitNamingStrategyLegacyJpaImpl.INSTANCE;
		}
	}

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		Postman postman = new Postman( "Bob", "A01" );
		House house = new House();
		house.setPostman( postman );
		house.setAddress( "Rue des pres" );
		scope.inTransaction(
				s -> {
					s.persist( postman );
					s.persist( house );
				}
		);

		scope.inTransaction(
				s -> {
					House h = s.find( House.class, house.getId() );
					assertThat( h.getPostman() ).isNotNull();
					assertThat( h.getPostman().getName() ).isEqualTo( "Bob" );
					Postman pm = h.getPostman();
					s.remove( h );
					s.remove( pm );
				}
		);
	}

	@Test
	public void testOneToMany(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Rambler rambler = new Rambler( "Emmanuel" );
					Bag bag = new Bag( "0001", rambler );
					rambler.getBags().add( bag );
					s.persist( rambler );
				}
		);

		scope.inTransaction(
				s -> {
					Bag bag = s.createQuery( "select b from Bag b left join fetch b.owner", Bag.class )
							.uniqueResult();
					assertThat( bag ).isNotNull();
					assertThat( bag.getOwner() ).isNotNull();

					Rambler rambler = s.createQuery( "select r from Rambler r left join fetch r.bags", Rambler.class )
							.uniqueResult();
					assertThat( rambler ).isNotNull();
					assertThat( rambler.getBags() ).isNotNull();
					assertThat( rambler.getBags().size() ).isEqualTo( 1 );
					s.remove( rambler.getBags().iterator().next() );
					s.remove( rambler );
				}
		);
	}

	@Test
	public void testUnidirectionalOneToMany(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Clothes clothes = new Clothes( "underwear", "interesting" );
					Luggage luggage = new Luggage( "Emmanuel", "Cabin Luggage" );
					luggage.getHasInside().add( clothes );
					s.persist( luggage );
				}
		);

		scope.inTransaction(
				s -> {
					Luggage luggage = s.createQuery( "select l from Luggage l left join fetch l.hasInside", Luggage.class )
							.uniqueResult();
					assertThat( luggage ).isNotNull();
					assertThat( luggage.getHasInside() ).isNotNull();
					assertThat( luggage.getHasInside().size() ).isEqualTo( 1 );

					s.remove( luggage.getHasInside().iterator().next() );
					s.remove( luggage );

				}
		);
	}

	@Test
	public void testManyToMany(SessionFactoryScope scope) {
		House wh = new House();
		Inhabitant b = new Inhabitant();
		scope.inTransaction(
				session -> {
					wh.setAddress( "1600 Pennsylvania Avenue, Washington" );
					b.setName( "Bill Clinton" );
					Inhabitant george = new Inhabitant();
					george.setName( "George W Bush" );
					session.persist( george );
					session.persist( b );
					wh.getHasInhabitants().add( b );
					wh.getHasInhabitants().add( george );
					//bill.getLivesIn().add( whiteHouse );
					//george.getLivesIn().add( whiteHouse );

					session.persist( wh );
				}
		);

		scope.inTransaction(
				session -> {
					House whiteHouse = session.find( House.class, wh.getId() );
					assertThat( whiteHouse ).isNotNull();
					assertThat( whiteHouse.getHasInhabitants().size() ).isEqualTo( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					Inhabitant bill = session.find( Inhabitant.class, b.getId() );
					assertThat( bill ).isNotNull();
					assertThat( bill.getLivesIn().size() ).isEqualTo( 1 );
					assertThat( bill.getLivesIn().iterator().next().getAddress() ).isEqualTo( wh.getAddress() );

					House whiteHouse = bill.getLivesIn().iterator().next();
					session.remove( whiteHouse );
					for ( Inhabitant inhabitant : whiteHouse.getHasInhabitants() ) {
						session.remove( inhabitant );
					}
				}
		);
	}

	@Test
	public void testManyToOneReferenceManyToOne(SessionFactoryScope scope) {
		Item item = new Item();
		item.setId( 1 );
		Vendor vendor = new Vendor();
		vendor.setId( 1 );
		ItemCost cost = new ItemCost();
		cost.setCost( new BigDecimal( 1 ) );
		cost.setId( 1 );
		cost.setItem( item );
		cost.setVendor( vendor );
		WarehouseItem wItem = new WarehouseItem();
		wItem.setDefaultCost( cost );
		wItem.setId( 1 );
		wItem.setItem( item );
		wItem.setQtyInStock( new BigDecimal( 1 ) );
		wItem.setVendor( vendor );
		scope.inTransaction(
				s -> {
					s.persist( item );
					s.persist( vendor );
					s.persist( cost );
					s.persist( wItem );
					s.flush();
					s.clear();
					WarehouseItem warehouseItem = s.find( WarehouseItem.class, wItem.getId() );
					assertThat( warehouseItem.getDefaultCost().getItem() ).isNotNull();
				}
		);
	}

	@Test
	public void testManyToOneInsideComponentReferencedColumn(SessionFactoryScope scope) {
		HousePlaces house = new HousePlaces();
		house.places = new Places();

		house.places.livingRoom = new Place();
		house.places.livingRoom.name = "First";
		house.places.livingRoom.owner = "mine";

		house.places.kitchen = new Place();
		house.places.kitchen.name = "Kitchen 1";

		house.neighbourPlaces = new Places();
		house.neighbourPlaces.livingRoom = new Place();
		house.neighbourPlaces.livingRoom.name = "Neighbour";
		house.neighbourPlaces.livingRoom.owner = "his";

		house.neighbourPlaces.kitchen = new Place();
		house.neighbourPlaces.kitchen.name = "His Kitchen";

		scope.inTransaction(
				s -> {
					s.persist( house );
					s.flush();

					HousePlaces housePlaces = s.find( HousePlaces.class, house.id );
					assertThat( housePlaces.id ).isEqualTo( house.id );

					HousePlaces uniqueResult = s.createQuery(
									"from HousePlaces h where h.places.livingRoom.name='First'", HousePlaces.class )
							.uniqueResult();
					assertThat( uniqueResult ).isNotNull();
					assertThat( uniqueResult.places.livingRoom.name ).isEqualTo( "First" );
					assertThat( uniqueResult.places.livingRoom.owner ).isEqualTo( "mine" );

					uniqueResult = s.createQuery(
									"from HousePlaces h where h.places.livingRoom.owner=:owner", HousePlaces.class )
							.setParameter( "owner", "mine" ).uniqueResult();
					assertThat( uniqueResult ).isNotNull();
					assertThat( uniqueResult.places.livingRoom.name ).isEqualTo( "First" );
					assertThat( uniqueResult.places.livingRoom.owner ).isEqualTo( "mine" );

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<HousePlaces> criteria = criteriaBuilder.createQuery( HousePlaces.class );
					Root<HousePlaces> root = criteria.from( HousePlaces.class );
					Join<Object, Object> join = root.join( "places" ).join( "livingRoom" );
					criteria.where( criteriaBuilder.equal( join.get( "owner" ), "mine" ) );

					assertThat( s.createQuery( criteria ).uniqueResult() ).isNotNull();

//					assertNotNull( s.createCriteria( HousePlaces.class ).add( Restrictions.eq( "places.livingRoom.owner", "mine" ) )
//										   .uniqueResult() );

					// override
					uniqueResult = s.createQuery(
									"from HousePlaces h where h.neighbourPlaces.livingRoom.owner='his'", HousePlaces.class )
							.uniqueResult();
					assertThat( uniqueResult ).isNotNull();
					assertThat( uniqueResult.neighbourPlaces.livingRoom.name ).isEqualTo( "Neighbour" );
					assertThat( uniqueResult.neighbourPlaces.livingRoom.owner ).isEqualTo( "his" );

					uniqueResult = s.createQuery(
									"from HousePlaces h where h.neighbourPlaces.livingRoom.name=:name", HousePlaces.class )
							.setParameter( "name", "Neighbour" ).uniqueResult();
					assertThat( uniqueResult ).isNotNull();
					assertThat( uniqueResult.neighbourPlaces.livingRoom.name ).isEqualTo( "Neighbour" );
					assertThat( uniqueResult.neighbourPlaces.livingRoom.owner ).isEqualTo( "his" );

					criteria = criteriaBuilder.createQuery( HousePlaces.class );
					root = criteria.from( HousePlaces.class );
					join = root.join( "neighbourPlaces" ).join( "livingRoom" );
					criteria.where( criteriaBuilder.equal( join.get( "owner" ), "his" ) );

					assertThat( s.createQuery( criteria ).uniqueResult() ).isNotNull();

//					assertNotNull( s.createCriteria( HousePlaces.class )
//										   .add( Restrictions.eq( "neighbourPlaces.livingRoom.owner", "his" ) ).uniqueResult() );
					s.remove( house );

				}
		);
	}

}
