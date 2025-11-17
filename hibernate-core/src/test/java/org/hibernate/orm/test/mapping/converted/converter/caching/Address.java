/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.caching;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * @author Steve Ebersole
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Address {
	@Id
	private Integer id;
	private String streetLine1;
	private String streetLine2;
	@Convert(converter = PostalAreaConverter.class)
	private PostalArea postalArea;

	public Address() {
	}

	public Address(
			Integer id,
			String streetLine1,
			String streetLine2,
			PostalArea postalArea) {
		this.id = id;
		this.streetLine1 = streetLine1;
		this.streetLine2 = streetLine2;
		this.postalArea = postalArea;
	}

	public Integer getId() {
		return id;
	}

	public String getStreetLine1() {
		return streetLine1;
	}

	public void setStreetLine1(String streetLine1) {
		this.streetLine1 = streetLine1;
	}

	public String getStreetLine2() {
		return streetLine2;
	}

	public void setStreetLine2(String streetLine2) {
		this.streetLine2 = streetLine2;
	}

	public PostalArea getPostalArea() {
		return postalArea;
	}

	public void setPostalArea(PostalArea postalArea) {
		this.postalArea = postalArea;
	}
}
