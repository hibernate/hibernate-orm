/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.compound;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity(name = "PostalCarrier")
public class PostalCarrier {
	@Id
	private Long id;

	@NaturalId
	@ManyToOne
	private Country country;

	@NaturalId
	private String code;


	public PostalCarrier() {
	}

	public PostalCarrier(long id, String code, Country country) {
		this.id = id;
		this.code = code;
		this.country = country;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}
}
