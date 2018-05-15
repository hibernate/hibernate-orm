/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.lob;

import javax.persistence.Cacheable;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

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
	Integer id;
	String streetLine1;
	String streetLine2;
	@Lob
	@Convert(converter = PostalAreaConverter.class)
	PostalArea postalArea;

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
