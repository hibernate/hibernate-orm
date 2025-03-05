/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.*;

import java.util.Set;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "PHONE_TABLE")
public class Phone implements java.io.Serializable {
	public enum Type { LAND_LINE, CELL, FAX, WORK, HOME }

	private String id;
	private String area;
	private String number;
	private Address address;
	private Set<Type> types;

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
	public Set<Type> getTypes() {
		return types;
	}

	public void setTypes(Set<Type> types) {
		this.types = types;
	}
}
