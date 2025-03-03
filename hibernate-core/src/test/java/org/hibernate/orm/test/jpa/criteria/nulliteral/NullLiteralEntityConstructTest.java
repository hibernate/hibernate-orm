/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nulliteral;

import java.util.List;

import org.hibernate.annotations.Imported;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		NullLiteralEntityConstructTest.City.class,
		NullLiteralEntityConstructTest.Country.class,
		NullLiteralEntityConstructTest.CityProjection.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17135" )
public class NullLiteralEntityConstructTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new City( "London", "England", new Country( "United Kingdom", "GBR" ) ) );
			session.persist( new City( "Rome", "Lazio", new Country( "Italy", "ITA" ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from City" ).executeUpdate();
			session.createMutationQuery( "delete from Country" ).executeUpdate();
		} );
	}

	@Test
	public void testNullLiteralSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Tuple> cq = cb.createQuery( Tuple.class );
			final Root<City> cityRoot = cq.from( City.class );
			cq.multiselect( cityRoot.get( "name" ), cb.nullLiteral( Country.class ) )
					.where( cb.equal( cityRoot.get( "name" ), "London" ) );
			final Tuple result = session.createQuery( cq ).getSingleResult();
			assertThat( result.get( 0 ) ).isEqualTo( "London" );
			assertThat( result.get( 1 ) ).isNull();
		} );
	}

	@Test
	public void testNullLiteralConstruct(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<CityProjection> cq = cb.createQuery( CityProjection.class );
			final Root<City> cityRoot = cq.from( City.class );
			cq.select( cb.construct(
					CityProjection.class,
					cityRoot.get( "id" ),
					cityRoot.get( "name" ),
					cityRoot.get( "state" ),
					cb.nullLiteral( Country.class )
			) ).where( cb.equal( cityRoot.get( "name" ), "London" ) );
			final CityProjection result = session.createQuery( cq ).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "London" );
			assertThat( result.getCountryName() ).isNull();
		} );
	}

	@Test
	public void testSimpleConstruct(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<CityProjection> cq = cb.createQuery( CityProjection.class );
			final Root<City> root = cq.from( City.class );
			cq.select( cb.construct(
					CityProjection.class,
					root.get( "id" ),
					root.get( "name" ),
					root.get( "state" ),
					root.get( "country" )
			) ).where( cb.equal( root.get( "name" ), "London" ) );
			final CityProjection result = session.createQuery( cq ).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "London" );
			assertThat( result.getCountryName() ).isEqualTo( "United Kingdom" );
		} );
	}

	@Test
	public void testSetNullLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaUpdate<City> cu = cb.createCriteriaUpdate( City.class );
			final Root<City> root = cu.from( City.class );
			cu.set( "country", cb.nullLiteral( Country.class ) ).where( cb.equal( root.get( "name" ), "Rome" ) );
			session.createMutationQuery( cu ).executeUpdate();
		} );
		scope.inTransaction( session -> assertThat(
				session.createQuery( "where name = 'Rome'", City.class )
						.getSingleResult()
						.getCountry() ).isNull()
		);
	}

	@Entity( name = "City" )
	public static class City {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private String state;

		@ManyToOne( cascade = CascadeType.PERSIST )
		@JoinColumn( name = "country_id" )
		private Country country;

		public City() {
		}

		public City(String name, String state, Country country) {
			this.name = name;
			this.state = state;
			this.country = country;
		}

		public Country getCountry() {
			return country;
		}
	}

	@Imported
	public static class CityProjection {
		private Long id;
		private String name;
		private String state;
		private String countryName;

		public CityProjection(Long id, String name, String state, Country country) {
			this.id = id;
			this.name = name;
			this.state = state;
			if ( country != null ) {
				this.countryName = country.getName();
			}
		}

		public String getName() {
			return name;
		}

		public String getCountryName() {
			return countryName;
		}
	}


	@Entity( name = "Country" )
	public static class Country {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private String isoCode;

		@OneToMany( mappedBy = "country" )
		private List<City> cities;

		public Country() {
		}

		public Country(String name, String isoCode) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
