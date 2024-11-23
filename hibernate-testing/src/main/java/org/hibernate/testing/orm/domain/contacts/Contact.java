/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.contacts;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "contacts" )
@SecondaryTable( name="contact_supp" )
public class Contact {
	private Integer id;
	private Name name;
	private Gender gender;

	private LocalDate birthDay;

	private List<Address> addresses;
	private List<PhoneNumber> phoneNumbers;

	public Contact() {
	}

	public Contact(Integer id, Name name, Gender gender, LocalDate birthDay) {
		this.id = id;
		this.name = name;
		this.gender = gender;
		this.birthDay = birthDay;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	@Temporal( TemporalType.DATE )
	@Column( table = "contact_supp" )
	public LocalDate getBirthDay() {
		return birthDay;
	}

	public void setBirthDay(LocalDate birthDay) {
		this.birthDay = birthDay;
	}

	@ElementCollection
	@CollectionTable( name = "contact_addresses" )
	// NOTE : because of the @OrderColumn `addresses` is a List, while `phoneNumbers` is
	// 		a BAG which is a List with no persisted order
	@OrderColumn
	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	@ElementCollection
	@CollectionTable( name = "contact_phones" )
	public List<PhoneNumber> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	@Embeddable
	public static class Name {
		private String first;
		private String last;

		public Name() {
		}

		public Name(String first, String last) {
			this.first = first;
			this.last = last;
		}

		@Column(name = "firstname")
		public String getFirst() {
			return first;
		}

		public void setFirst(String first) {
			this.first = first;
		}

		@Column(name = "lastname")
		public String getLast() {
			return last;
		}

		public void setLast(String last) {
			this.last = last;
		}
	}

	public enum Gender {
		MALE,
		FEMALE,
		OTHER
	}
}
