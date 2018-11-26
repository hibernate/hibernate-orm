/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.LazyInitializationException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class ManyToOneLazyLoadingByIdTest extends BaseEntityManagerFunctionalTestCase {

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Continent.class,
				Country.class
		};
	}

	@Test
	public void testLazyLoadById() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Continent continent = new Continent();
			continent.setId( 1L );
			continent.setName( "Europe" );

			entityManager.persist( continent );

			Country country = new Country();
			country.setId( 1L );
			country.setName( "Romania" );
			country.setContinent( continent );

			entityManager.persist( country );
		} );

		Continent continent = doInJPA( this::entityManagerFactory, entityManager -> {
			Country country = entityManager.find( Country.class, 1L );

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
