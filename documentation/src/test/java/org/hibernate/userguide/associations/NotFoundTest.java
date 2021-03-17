package org.hibernate.userguide.associations;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Fábio Ueno
 */
public class NotFoundTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				City.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::associations-not-found-persist-example[]
			City _NewYork = new City();
			_NewYork.setName( "New York" );
			entityManager.persist( _NewYork );

			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );
			person.setCityName( "New York" );
			entityManager.persist( person );
			//end::associations-not-found-persist-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::associations-not-found-find-example[]
			Person person = entityManager.find( Person.class, 1L );
			assertEquals( "New York", person.getCity().getName() );
			//end::associations-not-found-find-example[]

			//tag::associations-not-found-non-existing-persist-example[]
			person.setCityName( "Atlantis" );
			//end::associations-not-found-non-existing-persist-example[]

		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::associations-not-found-non-existing-find-example[]
			Person person = entityManager.find( Person.class, 1L );

			assertEquals( "Atlantis", person.getCityName() );
			assertNull( null, person.getCity() );
			//end::associations-not-found-non-existing-find-example[]
		} );
	}

	//tag::associations-not-found-domain-model-example[]
	@Entity
	@Table( name = "Person" )
	public static class Person {

		@Id
		private Long id;

		private String name;

		private String cityName;

		@ManyToOne
		@NotFound ( action = NotFoundAction.IGNORE )
		@JoinColumn(
			name = "cityName",
			referencedColumnName = "name",
			insertable = false,
			updatable = false
		)
		private City city;

		//Getters and setters are omitted for brevity

	//end::associations-not-found-domain-model-example[]

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

		public String getCityName() {
			return cityName;
		}

		public void setCityName(String cityName) {
			this.cityName = cityName;
			this.city = null;
		}

		public City getCity() {
			return city;
		}
	//tag::associations-not-found-domain-model-example[]
	}

	@Entity
	@Table( name = "City" )
	public static class City implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		//Getters and setters are omitted for brevity

	//end::associations-not-found-domain-model-example[]

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
	//tag::associations-not-found-domain-model-example[]
	}
	//end::associations-not-found-domain-model-example[]
}