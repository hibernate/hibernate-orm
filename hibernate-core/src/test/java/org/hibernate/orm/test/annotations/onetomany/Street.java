/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Formula;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Street {
	private Integer id;
	private String streetName;
	private String streetNameCopy;
	private City city;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name="STREET_NAME")
	public String getStreetName() {
		return streetName;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	@Formula("STREET_NAME")
	public String getStreetNameCopy() {
		return streetNameCopy;
	}

	public void setStreetNameCopy(String streetNameCopy) {
		this.streetNameCopy = streetNameCopy;
	}

	@ManyToOne
	public City getCity() {
		return city;
	}

	public void setCity(City city) {
		this.city = city;
	}
}
