/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MapKeyClassTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::collections-map-key-class-persist-example[]
			Person person = new Person();
			person.setId( 1L );
			person.getCallRegister().put( new MobilePhone( "01", "234", "567" ), 101 );
			person.getCallRegister().put( new MobilePhone( "01", "234", "789" ), 102 );

			entityManager.persist( person );
			//end::collections-map-key-class-persist-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::collections-map-key-class-fetch-example[]
			Person person = entityManager.find( Person.class, 1L );
			assertEquals( 2, person.getCallRegister().size() );

			assertEquals(
				Integer.valueOf( 101 ),
				person.getCallRegister().get( MobilePhone.fromString( "01-234-567" ) )
			);

			assertEquals(
				Integer.valueOf( 102 ),
				person.getCallRegister().get( MobilePhone.fromString( "01-234-789" ) )
			);
			//end::collections-map-key-class-fetch-example[]
		} );
	}

	//tag::collections-map-key-class-mapping-example[]
	@Entity
	@Table(name = "person")
	public static class Person {

		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(
			name = "call_register",
			joinColumns = @JoinColumn(name = "person_id")
		)
		@MapKeyColumn( name = "call_timestamp_epoch" )
		@MapKeyClass( MobilePhone.class )
		@Column(name = "call_register")
		private Map<PhoneNumber, Integer> callRegister = new HashMap<>();

		//Getters and setters are omitted for brevity
	//end::collections-map-key-class-mapping-example[]

		public void setId(Long id) {
			this.id = id;
		}

		public Map<PhoneNumber, Integer> getCallRegister() {
			return callRegister;
		}
	//tag::collections-map-key-class-mapping-example[]
	}
	//end::collections-map-key-class-mapping-example[]

	//tag::collections-map-key-class-type-mapping-example[]
	public interface PhoneNumber {

		String get();
	}

	@Embeddable
	public static class MobilePhone
			implements PhoneNumber {

		static PhoneNumber fromString(String phoneNumber) {
			String[] tokens = phoneNumber.split( "-" );
			if ( tokens.length != 3 ) {
				throw new IllegalArgumentException( "invalid phone number: " + phoneNumber );
			}
			int i = 0;
			return new MobilePhone(
				tokens[i++],
				tokens[i++],
				tokens[i]
			);
		}

		private MobilePhone() {
		}

		public MobilePhone(
				String countryCode,
				String operatorCode,
				String subscriberCode) {
			this.countryCode = countryCode;
			this.operatorCode = operatorCode;
			this.subscriberCode = subscriberCode;
		}

		@Column(name = "country_code")
		private String countryCode;

		@Column(name = "operator_code")
		private String operatorCode;

		@Column(name = "subscriber_code")
		private String subscriberCode;

		@Override
		public String get() {
			return String.format(
				"%s-%s-%s",
				countryCode,
				operatorCode,
				subscriberCode
			);
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MobilePhone that = (MobilePhone) o;
			return Objects.equals( countryCode, that.countryCode ) &&
					Objects.equals( operatorCode, that.operatorCode ) &&
					Objects.equals( subscriberCode, that.subscriberCode );
		}

		@Override
		public int hashCode() {
			return Objects.hash( countryCode, operatorCode, subscriberCode );
		}
	}
	//end::collections-map-key-class-type-mapping-example[]
}
