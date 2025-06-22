/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static jakarta.persistence.DiscriminatorType.STRING;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		CriteriaInheritanceJoinTest.Address.class,
		CriteriaInheritanceJoinTest.StreetAddress.class,
		CriteriaInheritanceJoinTest.Street.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16719" )
public class CriteriaInheritanceJoinTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Street street = new Street( "Via Roma" );
			session.persist( street );
			final StreetAddress streetAddress = new StreetAddress( 1, "A", street );
			session.persist( streetAddress );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from StreetAddress" ).executeUpdate();
			session.createMutationQuery( "delete from Street" ).executeUpdate();
		} );
	}

	@Test
	public void findStreet(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Street> streetCriteriaQuery = cb.createQuery( Street.class );
			final Root<Street> streetRoot = streetCriteriaQuery.from( Street.class );
			streetCriteriaQuery.select( streetRoot ).where( cb.equal( streetRoot.get( "name" ), "Via Roma" ) );
			final Street result = session.createQuery( streetCriteriaQuery ).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "Via Roma" );
		} );
	}

	@Test
	public void findAddressImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Address> cq = cb.createQuery( Address.class );
			final Root<Address> addressRoot = cq.from( Address.class );
			cq.select( addressRoot ).where(
					cb.equal( cb.treat( addressRoot, StreetAddress.class )
									.get( "street" )
									.get( "name" ), "Via Roma" )
			);
			final Address result = session.createQuery( cq ).getSingleResult();
			assertThat( result ).isInstanceOf( StreetAddress.class );
			assertThat( ( (StreetAddress) result ).getStreet().getName() ).isEqualTo( "Via Roma" );
		} );
	}

	@Test
	public void findAddressExplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Address> cq = cb.createQuery( Address.class );
			final Root<Address> addressRoot = cq.from( Address.class );
			final Join<Address, Street> join = cb.treat( addressRoot, StreetAddress.class ).join( "street" );
			cq.select( addressRoot ).where( cb.equal( join.get( "name" ), "Via Roma" ) );
			final Address result = session.createQuery( cq ).getSingleResult();
			assertThat( result ).isInstanceOf( StreetAddress.class );
			assertThat( ( (StreetAddress) result ).getStreet().getName() ).isEqualTo( "Via Roma" );
		} );
	}

	@Entity( name = "Address" )
	@Inheritance( strategy = SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = STRING )
	public static class Address {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity( name = "StreetAddress" )
	@DiscriminatorValue( "O" )
	public static class StreetAddress extends Address {
		private Integer houseNumber;
		private String houseLetter;
		@OneToOne
		private Street street;

		public StreetAddress() {
		}

		public StreetAddress(Integer houseNumber, String houseLetter, Street street) {
			this.houseNumber = houseNumber;
			this.houseLetter = houseLetter;
			this.street = street;
		}

		public Integer getHouseNumber() {
			return houseNumber;
		}

		public String getHouseLetter() {
			return houseLetter;
		}

		public Street getStreet() {
			return street;
		}
	}

	@Entity( name = "Street" )
	public static class Street {
		@Id
		@GeneratedValue
		private Long id;
		private String name;

		public Street() {
		}

		public Street(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
