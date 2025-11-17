/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embedded.many2one;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * THe entity target of the many-to-one from a component/embeddable.
 *
 * @author Steve Ebersole
 */
@Entity
public class Country implements Serializable {
	private String iso2;
	private String name;

	public Country() {
	}

	public Country(String iso2, String name) {
		this.iso2 = iso2;
		this.name = name;
	}

	@Id
	public String getIso2() {
		return iso2;
	}

	public void setIso2(String iso2) {
		this.iso2 = iso2;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
