/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.hql;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				FilterWithDifferentConditionsTest.Person.class,
				FilterWithDifferentConditionsTest.Vehicle.class
		}
)
@SessionFactory(exportSchema = true)
@JiraKey(value = "HHH-13485")
public class FilterWithDifferentConditionsTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Vehicle bike = new Vehicle( 1, "bike" );
					Vehicle car = new Vehicle( 2, "car" );

					Person person = new Person( 3 );
					person.addOwned( "bike", bike );
					person.addOwned( "car", car );

					person.addRented( "bike", bike );
					person.addRented( "car", car );

					session.persist( bike );
					session.persist( car );
					session.persist( person );
				}
		);
	}

	@Test
	public void testFilterWithDefaultCondition(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.enableFilter( "addressFilter" );
					List<Person> people = session.createQuery( "from Person", Person.class ).list();
					assertThat( people.size() ).isEqualTo( 1 );

					Person person = people.get( 0 );

					Collection<Vehicle> owned = person.getOwned();
					assertThat( owned.size() ).isEqualTo( 1 );

					Collection<Vehicle> rented = person.getRented();
					assertThat( rented.size() ).isEqualTo( 1 );

					assertThat( owned.iterator().next().getType() ).isEqualTo( "car" );
					assertThat( rented.iterator().next().getType() ).isEqualTo( "bike" );
				}
		);
	}


	@Entity(name = "Person")
	@FilterDef(name = "addressFilter", defaultCondition = "(CAR_TYPE = 'car')")
	public static class Person {

		@Id
		private Integer id;

		@ManyToMany
		@MapKeyColumn(name = "CAR_TYPE")
		@JoinTable(name = "PERSON_RENTED")
		@FilterJoinTable(name = "addressFilter", condition = "(CAR_TYPE = 'bike')")
		private Map<String, Vehicle> rented = new HashMap<>();

		@ManyToMany
		@JoinTable(name = "PERSON_OWNED")
		@MapKeyColumn(name = "CAR_TYPE")
		@FilterJoinTable(name = "addressFilter")
		private Map<String, Vehicle> owned = new HashMap<>();

		public Person() {
		}

		public Person(Integer id) {
			this.id = id;
		}

		public void addRented(String type, Vehicle vehicle) {
			this.rented.put( type, vehicle );
		}

		public void addOwned(String type, Vehicle vehicle) {
			this.owned.put( type, vehicle );
		}

		public Integer getId() {
			return id;
		}

		public Collection<Vehicle> getRented() {
			return rented.values();
		}

		public Collection<Vehicle> getOwned() {
			return owned.values();
		}
	}

	@Entity(name = "Vehicle")
	public static class Vehicle {

		@Id
		private Integer id;

		@Column(name = "CAR_TYPE")
		private String type;

		public Vehicle() {
		}

		public Vehicle(Integer id, String type) {
			this.id = id;
			this.type = type;
		}

		public Integer getId() {
			return id;
		}

		public String getType() {
			return type;
		}
	}
}
