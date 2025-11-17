/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Jpa(
		annotatedClasses = {
				ExpressionsTest.Address.class,
				ExpressionsTest.Phone.class
		},
		properties = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class ExpressionsTest {

	@Test
	public void testJoinedElementCollectionValuesInTupleList(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Phone> criteria = builder.createQuery( Phone.class );
					Root<Phone> from = criteria.from( Phone.class );
					criteria.where(
							from.join( "types" )
									.in( Collections.singletonList( Phone.Type.WORK ) )
					);
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}

	@Entity(name = "Phone")
	@Table(name = "PHONE_TABLE")
	public static class Phone implements java.io.Serializable {
		public enum Type {LAND_LINE, CELL, FAX, WORK, HOME}

		private String id;
		private String area;
		private String number;
		private Address address;
		private Set<Phone.Type> types;

		public Phone() {
		}

		public Phone(String v1, String v2, String v3) {
			id = v1;
			area = v2;
			number = v3;
		}

		public Phone(String v1, String v2, String v3, Address v4) {
			id = v1;
			area = v2;
			number = v3;
			address = v4;
		}

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String v) {
			id = v;
		}

		@Column(name = "AREA")
		public String getArea() {
			return area;
		}

		public void setArea(String v) {
			area = v;
		}

		@Column(name = "PHONE_NUMBER")
		public String getNumber() {
			return number;
		}

		public void setNumber(String v) {
			number = v;
		}

		@ManyToOne
		@JoinColumn(name = "FK_FOR_ADDRESS")
		public Address getAddress() {
			return address;
		}

		public void setAddress(Address a) {
			address = a;
		}

		@ElementCollection
		public Set<Phone.Type> getTypes() {
			return types;
		}

		public void setTypes(Set<Phone.Type> types) {
			this.types = types;
		}
	}

	@Entity
	@Table(name = "ADDRESS")
	public static class Address implements java.io.Serializable {
		private String id;
		private String street;
		private String city;
		private String state;
		private String zip;
		private List<Phone> phones = new java.util.ArrayList<>();

		public Address() {
		}

		public Address(String id, String street, String city, String state, String zip) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
		}

		public Address(
				String id, String street, String city, String state, String zip,
				List<Phone> phones) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
			this.phones = phones;
		}

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Column(name = "STREET")
		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		@Column(name = "CITY")
		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		@Column(name = "STATE")
		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		@Column(name = "ZIP")
		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "address")
		@OrderColumn
		public List<Phone> getPhones() {
			return phones;
		}

		public void setPhones(List<Phone> phones) {
			this.phones = phones;
		}
	}

}
