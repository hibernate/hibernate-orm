/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JoinColumnOrFormulaTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( JoinColumnOrFormulaTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Country.class,
			User.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::mapping-JoinColumnOrFormula-persistence-example[]
		Country US = new Country();
		US.setId( 1 );
		US.setDefault( true );
		US.setPrimaryLanguage( "English" );
		US.setName( "United States" );

		Country Romania = new Country();
		Romania.setId( 40 );
		Romania.setDefault( true );
		Romania.setName( "Romania" );
		Romania.setPrimaryLanguage( "Romanian" );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( US );
			entityManager.persist( Romania );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			User user1 = new User( );
			user1.setId( 1L );
			user1.setFirstName( "John" );
			user1.setLastName( "Doe" );
			user1.setLanguage( "English" );
			entityManager.persist( user1 );

			User user2 = new User( );
			user2.setId( 2L );
			user2.setFirstName( "Vlad" );
			user2.setLastName( "Mihalcea" );
			user2.setLanguage( "Romanian" );
			entityManager.persist( user2 );

		} );
		//end::mapping-JoinColumnOrFormula-persistence-example[]

		//tag::mapping-JoinColumnOrFormula-fetching-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Fetch User entities" );

			User john = entityManager.find( User.class, 1L );
			assertEquals( US, john.getCountry());

			User vlad = entityManager.find( User.class, 2L );
			assertEquals( Romania, vlad.getCountry());
		} );
		//end::mapping-JoinColumnOrFormula-fetching-example[]
	}

	//tag::mapping-JoinColumnOrFormula-example[]
	@Entity(name = "User")
	@Table(name = "users")
	public static class User {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		private String language;

		@ManyToOne
		@JoinColumnOrFormula( column =
			@JoinColumn(
				name = "language",
				referencedColumnName = "primaryLanguage",
				insertable = false,
				updatable = false
			)
		)
		@JoinColumnOrFormula( formula =
			@JoinFormula(
				value = "true",
				referencedColumnName = "is_default"
			)
		)
		private Country country;

		//Getters and setters omitted for brevity

	//end::mapping-JoinColumnOrFormula-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}

		//tag::mapping-JoinColumnOrFormula-example[]
	}

	@Entity(name = "Country")
	@Table(name = "countries")
	public static class Country implements Serializable {

		@Id
		private Integer id;

		private String name;

		private String primaryLanguage;

		@Column(name = "is_default")
		private boolean _default;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPrimaryLanguage() {
			return primaryLanguage;
		}

		public void setPrimaryLanguage(String primaryLanguage) {
			this.primaryLanguage = primaryLanguage;
		}

		public boolean isDefault() {
			return _default;
		}

		public void setDefault(boolean _default) {
			this._default = _default;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Country ) ) {
				return false;
			}
			Country country = (Country) o;
			return Objects.equals( getId(), country.getId() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getId() );
		}
	}
	//end::mapping-JoinColumnOrFormula-example[]
}
