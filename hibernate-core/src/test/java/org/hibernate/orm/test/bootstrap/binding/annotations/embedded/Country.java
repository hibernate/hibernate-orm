/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import java.io.Serializable;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import jakarta.persistence.Access;

/**
 * Non realistic embedded dependent object
 *
 * @author Emmanuel Bernard
 */
@Embeddable
@Access(AccessType.PROPERTY)
public class Country implements Serializable {
	private String iso2;
	private String name;

	public String getIso2() {
		return iso2;
	}

	public void setIso2(String iso2) {
		this.iso2 = iso2;
	}

	@Column(name = "countryName")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
