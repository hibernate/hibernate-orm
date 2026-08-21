/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletedetached;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-4160")
@Jpa(annotatedClasses = {
		DeleteDetachedJpaComplianceTest.RestaurantWithCompositeKey.class,
		DeleteDetachedJpaComplianceTest.Restaurant.class
})
public class DeleteDetachedJpaComplianceTest {
	@Test
	void testComposite(EntityManagerFactoryScope scope) {
		RestaurantWithCompositeKey restaurant = new RestaurantWithCompositeKey();
		restaurant.name = "Some stuff about the thing";
		scope.inTransaction( s -> s.persist( restaurant ) );
		scope.inTransaction( s -> {
			RestaurantWithCompositeKey otherRestaurant = s.find(
					RestaurantWithCompositeKey.class,
					new RestaurantPK( restaurant.regionId, restaurant.restaurantId )
			);
			assertNotNull( otherRestaurant );
			assertThrows(IllegalArgumentException.class,
					() -> s.remove( restaurant ),
					"Given entity is not associated with the persistence context"
			);
		} );
		scope.inTransaction( s -> {
			assertNotNull( s.find(
					RestaurantWithCompositeKey.class,
					new RestaurantPK( restaurant.regionId, restaurant.restaurantId )
			) );
		} );
	}

	@Test
	void testRegular(EntityManagerFactoryScope scope) {
		Restaurant restaurant = new Restaurant();
		restaurant.name = "Some stuff about the thing";
		scope.inTransaction( s -> s.persist( restaurant ) );
		scope.inTransaction( s -> {
			Restaurant otherRestaurant = s.find( Restaurant.class, restaurant.restaurantId );
			assertNotNull( otherRestaurant );
			assertThrows(IllegalArgumentException.class,
					() -> s.remove( restaurant ),
					"Given entity is not associated with the persistence context"
			);
		} );
		scope.inTransaction( s -> {
			assertNotNull( s.find( Restaurant.class, restaurant.restaurantId ) );
		} );
	}

	@Entity
	static class Restaurant {
		@Id
		@GeneratedValue
		long restaurantId;
		String name;
	}

	@Entity
	@IdClass(value = RestaurantPK.class)
	static class RestaurantWithCompositeKey {
		@Id
		@GeneratedValue
		long regionId;
		@Id
		@GeneratedValue
		long restaurantId;
		String name;
	}

	static class RestaurantPK {
		long regionId;
		long restaurantId;

		public RestaurantPK() {
		}

		public RestaurantPK(long regionId, long restaurantId) {
			this.regionId = regionId;
			this.restaurantId = restaurantId;
		}
	}
}
