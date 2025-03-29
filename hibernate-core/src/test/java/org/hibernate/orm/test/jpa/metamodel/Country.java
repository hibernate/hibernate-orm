/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;
import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Embeddable
public class Country implements java.io.Serializable {
	private String country;
	private String code;

	public Country() {
	}

	public Country(String v1, String v2) {
		country = v1;
		code = v2;
	}

	@Basic
	public String getCountry() {
		return country;
	}

	public void setCountry(String v) {
		country = v;
	}

	@Basic
	public String getCode() {
		return code;
	}

	public void setCode(String v) {
		code = v;
	}
}
