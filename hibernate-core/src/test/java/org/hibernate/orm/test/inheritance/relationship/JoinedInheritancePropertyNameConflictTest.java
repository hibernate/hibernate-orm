/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.relationship;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@JiraKey(value = "HHH-7406")
@DomainModel(
		annotatedClasses = {
				JoinedInheritancePropertyNameConflictTest.Country.class,
				JoinedInheritancePropertyNameConflictTest.Town.class,
				JoinedInheritancePropertyNameConflictTest.Mountain.class,
				JoinedInheritancePropertyNameConflictTest.Place.class
		}
)
public class JoinedInheritancePropertyNameConflictTest {

	@Test
	@FailureExpected(jiraKey = "HHH-7406")
	public void testQueryConflictingPropertyName(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Town town = new Town( 1L, "London", 5000000 );
					Country country = new Country( 2L, "Andorra", 10000 );
					Mountain mountain = new Mountain( 3L, "Mont Blanc", 4810 );
					session.persist( town );
					session.persist( country );
					session.persist( mountain );
				} );

		scope.inTransaction(
				session -> {
					List<Place> places = session.createQuery(
							"select pl from " + Place.class.getName() + " pl " +
									" where pl.population > 1000" )
							.getResultList();

					//Expected list of length 2. Expected London and Andorra
					assertEquals( 2L, places.size() );
				} );
	}

	@Entity
	@Table(name = "PLACE")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Place {

		@Id
		@Column(name = "PLACE_ID")
		private Long id;

		@Column(name = "PLACE_NAME")
		private String name;

		protected Place() {
		}

		protected Place(Long id, String name) {
			super();
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	@Table(name = "COUNTRY")
	@PrimaryKeyJoinColumn(name = "PLACE_ID", referencedColumnName = "PLACE_ID")
	public static class Country extends Place {

		@Column(name = "NU_POPULATION")
		private Integer population;

		public Country() {
		}

		public Country(Long id, String name, Integer population) {
			super( id, name );
			this.population = population;
		}
	}

	@Entity
	@Table(name = "MOUNTAIN")
	@PrimaryKeyJoinColumn(name = "PLACE_ID", referencedColumnName = "PLACE_ID")
	public static class Mountain extends Place {

		@Column(name = "NU_HEIGHT")
		private Integer height;

		public Mountain() {
		}

		public Mountain(Long id, String name, Integer height) {
			super( id, name );
			this.height = height;
		}
	}

	@Entity
	@Table(name = "TOWN")
	@PrimaryKeyJoinColumn(name = "PLACE_ID", referencedColumnName = "PLACE_ID")
	public static class Town extends Place {

		@Column(name = "NU_POPULATION")
		private Integer population;

		public Town() {
		}

		public Town(Long id, String name, Integer population) {
			super( id, name );
			this.population = population;
		}
	}
}
