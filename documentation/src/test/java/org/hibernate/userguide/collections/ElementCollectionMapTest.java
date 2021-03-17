/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class ElementCollectionMapTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( 1L );
			//tag::collections-map-value-type-entity-key-add-example[]
			person.getPhoneRegister().put(
				new Phone( PhoneType.LAND_LINE, "028-234-9876" ), new Date()
			);
			person.getPhoneRegister().put(
				new Phone( PhoneType.MOBILE, "072-122-9876" ), new Date()
			);
			//end::collections-map-value-type-entity-key-add-example[]
			entityManager.persist( person );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			Map<Phone, Date> phones = person.getPhoneRegister();
			Assert.assertEquals( 2, phones.size() );
		} );
	}

	//tag::collections-map-value-type-entity-key-example[]
	public enum PhoneType {
		LAND_LINE,
		MOBILE
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@Temporal(TemporalType.TIMESTAMP)
		@ElementCollection
		@CollectionTable(name = "phone_register")
		@Column(name = "since")
		private Map<Phone, Date> phoneRegister = new HashMap<>();

		//Getters and setters are omitted for brevity

	//end::collections-map-value-type-entity-key-example[]

		public Person() {}

		public Person(Long id) {
			this.id = id;
		}

		public Map<Phone, Date> getPhoneRegister() {
			return phoneRegister;
		}
	//tag::collections-map-value-type-entity-key-example[]
	}

	@Embeddable
	public static class Phone {

		private PhoneType type;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::collections-map-value-type-entity-key-example[]

		public Phone() {
		}

		public Phone(PhoneType type, String number) {
			this.type = type;
			this.number = number;
		}

		public PhoneType getType() {
			return type;
		}

		public String getNumber() {
			return number;
		}
	//tag::collections-map-value-type-entity-key-example[]
	}
	//end::collections-map-value-type-entity-key-example[]
}
