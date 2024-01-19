/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.annotations.inheritance.joined.JoinedSubclassRemovalByTypeTest.BaseEntity;
import static org.hibernate.orm.test.annotations.inheritance.joined.JoinedSubclassRemovalByTypeTest.BaseVehicle;
import static org.hibernate.orm.test.annotations.inheritance.joined.JoinedSubclassRemovalByTypeTest.Car;
import static org.hibernate.orm.test.annotations.inheritance.joined.JoinedSubclassRemovalByTypeTest.Truck;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@JiraKey("HHH-17667")
@DomainModel(
		annotatedClasses = {
				Car.class, Truck.class, BaseVehicle.class, BaseEntity.class
		}
)
@SessionFactory
class JoinedSubclassRemovalByTypeTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( Car.create( 1L ) );
			session.persist( Car.create( 2L ) );
			session.persist( Car.create( 3L ) );
			session.persist( Truck.create( 10L ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Car" ).executeUpdate();
			session.createQuery( "delete from Truck" ).executeUpdate();
		} );
	}

	@Test
	void testRemoval(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// NOTE that a delete query is created for BaseVehicle
					//   and not BaseEntity (where the @DiscriminatorColumn is present)
					session.createQuery( "delete from BaseVehicle e where type( e ) in (:type) " )
							.setParameter( "type", Car.class )
							.executeUpdate();
					// Trying to execute this query generates a SQL:
					//		insert into HT_BaseEntity(id)
					//		select bv1_0.id
					//		from BaseVehicle bv1_0
					//		         left join Car bv1_2 on bv1_0.id = bv1_2.id
					//		         left join Truck bv1_3 on bv1_0.id = bv1_3.id
					//		where bv1_1.type in (?)
					//
					//  but `bv1_1` references something that is not there ...
					//  leading to an exception
				}
		);

		scope.inTransaction(
				session -> {
					Long count = session.createQuery( "select count(v) from BaseVehicle v", Long.class )
							.getSingleResult();
					assertThat( count ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "Car")
	public static class Car extends BaseVehicle {
		public static Car create(Long id) {
			Car car = new Car();
			car.id = id;
			return car;
		}
	}

	@Entity(name = "Truck")
	public static class Truck extends BaseVehicle {
		public static Truck create(Long id) {
			Truck truck = new Truck();
			truck.id = id;
			return truck;
		}
	}

	@Entity(name = "BaseVehicle")
	public static class BaseVehicle extends BaseEntity {
	}

	@Entity(name = "BaseEntity")
	@DiscriminatorColumn(name = "type")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseEntity implements Serializable {
		@Id
		public Long id;
	}
}
