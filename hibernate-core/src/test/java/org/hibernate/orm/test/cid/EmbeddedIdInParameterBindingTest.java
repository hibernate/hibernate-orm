/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddedIdInParameterBindingTest.Delivery.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "true"))
@Jira( "HHH-19792" )
public class EmbeddedIdInParameterBindingTest {

	LocationId verbania = new LocationId( "Italy", "Verbania" );
	Delivery pizza = new Delivery( verbania, "Pizza Margherita" );

	LocationId hallein = new LocationId( "Austria", "Hallein" );
	Delivery schnitzel = new Delivery( hallein, "Wiener Schnitzel" );

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( pizza );
					session.persist( schnitzel );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testQueryWithWhereClauseContainingInOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Delivery> deliveries = session.createQuery( "from Delivery d where d.locationId in (?1)",
									Delivery.class )
							.setParameter( 1, verbania )
							.getResultList();
					assertThat( deliveries.size() ).isEqualTo( 1 );
					assertThat( deliveries ).contains( pizza );
				}
		);
	}

	@Test
	public void testQueryWithWhereClauseContainingInOperatorWithListOfParametersValues(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Delivery> deliveries = session.createQuery( "from Delivery d where d.locationId in ?1",
									Delivery.class )
							.setParameter( 1, List.of( verbania ) )
							.getResultList();
					assertThat( deliveries.size() ).isEqualTo( 1 );
					assertThat( deliveries ).contains( pizza );
				}
		);

		scope.inTransaction(
				session -> {
					List<Delivery> deliveries = session.createQuery( "from Delivery d where d.locationId in ?1",
									Delivery.class )
							.setParameter( 1, List.of( verbania, hallein ) )
							.getResultList();
					assertThat( deliveries.size() ).isEqualTo( 2 );
					assertThat( deliveries ).contains( pizza );
					assertThat( deliveries ).contains( schnitzel );
				}
		);
	}

	@Test
	public void testMoreComplexWhereClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Delivery> deliveries = session.createQuery(
									"from Delivery d where d.field2Copy = ?3 and d.locationId in (?1,?2) and d.field = ?3",
									Delivery.class )
							.setParameter( 1, verbania )
							.setParameter( 2, hallein )
							.setParameter( 3, "Pizza Margherita" )
							.getResultList();
					assertThat( deliveries.size() ).isEqualTo( 1 );
					assertThat( deliveries ).contains( pizza );
				}
		);
	}

	@Test
	public void testQueryWithWhereClauseContainingInOperatorAndTwoParamaters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Delivery> deliveries = session.createQuery( "from Delivery d where d.locationId in (?1,?2)",
									Delivery.class )
							.setParameter( 1, verbania )
							.setParameter( 2, hallein )
							.getResultList();
					assertThat( deliveries.size() ).isEqualTo( 2 );
					assertThat( deliveries ).contains( pizza );
					assertThat( deliveries ).contains( schnitzel );
				}
		);
	}

	@Entity(name = "Delivery")
	@Table(name = "Delivery")
	public static class Delivery {

		@EmbeddedId
		private LocationId locationId;

		@Column(name = "field")
		private String field;

		@Column(name = "field2")
		private String field2Copy;

		public Delivery() {
		}

		public Delivery(LocationId locationId, String field) {
			this.locationId = locationId;
			this.field = field;
			this.field2Copy = field;
		}

		public LocationId getLocationId() {
			return locationId;
		}

		public void setLocationId(LocationId locationId) {
			this.locationId = locationId;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		@Override
		public String toString() {
			return locationId + ":" + field;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Delivery table = (Delivery) o;
			return Objects.equals( locationId, table.locationId ) && Objects.equals( field, table.field );
		}

		@Override
		public int hashCode() {
			return Objects.hash( locationId, field );
		}
	}


	@Embeddable
	public static class LocationId {

		@Column(name = "sp_country")
		private String country;

		@Column(name = "sp_city")
		private String city;

		public LocationId(String country, String city) {
			this.country = country;
			this.city = city;
		}

		public LocationId() {
		}

		public String getCountry() {
			return country;
		}

		public String getCity() {
			return city;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public void setCity(String city) {
			this.city = city;
		}

		@Override
		public String toString() {
			return "[" + country + "-" + city + "]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			LocationId tableId = (LocationId) o;
			return Objects.equals( country, tableId.country ) && Objects.equals( city, tableId.city );
		}

		@Override
		public int hashCode() {
			return Objects.hash( country, city );
		}
	}
}
