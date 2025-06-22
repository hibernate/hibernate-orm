/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazyload;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.LazyInitializationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneLazyLoadingByIdTest.Continent.class,
				ManyToOneLazyLoadingByIdTest.Country.class
		}
)
@SessionFactory
public class ManyToOneLazyLoadingByIdTest {


	@Test
	public void testLazyLoadById(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Continent continent = new Continent();
			continent.setId( 1L );
			continent.setName( "Europe" );

			session.persist( continent );

			Country country = new Country();
			country.setId( 1L );
			country.setName( "Romania" );
			country.setContinent( continent );

			session.persist( country );
		} );

		Continent continent = scope.fromTransaction( session -> {
			Country country = session.find( Country.class, 1L );

			country.getContinent().getId();

			return country.getContinent();
		} );

		assertEquals( 1L, (long) continent.getId() );

		assertProxyState( continent );
	}

	protected void assertProxyState(Continent continent) {
		try {
			continent.getName();

			fail( "Should throw LazyInitializationException!" );
		}
		catch (LazyInitializationException expected) {

		}
	}

	@Entity(name = "Country")
	public static class Country {
		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Continent continent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Continent getContinent() {
			return continent;
		}

		public void setContinent(Continent continent) {
			this.continent = continent;
		}
	}

	@Entity(name = "Continent")
	public static class Continent {
		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
