/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;

@Embeddable @MappedSuperclass
class PostalCode {
	@Column(name="country_code")
	String countryCode;
	@Column(name="zip_code")
	int zipCode;
}
