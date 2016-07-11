/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.userguide.model.PhoneType;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class PhoneTypeEnumeratedStringTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Phone.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Phone phone = new Phone( );
			phone.setId( 1L );
			phone.setNumber( "123-456-78990" );
			phone.setType( PhoneType.MOBILE );
			entityManager.persist( phone );
		} );
	}

	//tag::basic-enums-Enumerated-string-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "phone_number")
		private String number;

		@Enumerated(EnumType.STRING)
		@Column(name = "phone_type")
		private PhoneType type;

		//Getters and setters are omitted for brevity

	//end::basic-enums-Enumerated-string-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public PhoneType getType() {
			return type;
		}

		public void setType(PhoneType type) {
			this.type = type;
		}
	//tag::basic-enums-Enumerated-string-example[]
	}
	//end::basic-enums-Enumerated-string-example[]
}
