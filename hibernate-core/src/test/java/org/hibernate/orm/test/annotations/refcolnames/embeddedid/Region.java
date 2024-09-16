/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
class Region {
	@EmbeddedId
	@AttributeOverride(name = "countryCode", column=@Column(name="region_country_code"))
	@AttributeOverride(name = "zipCode", column=@Column(name="region_zip_code"))
	PostalCode postalCode;
}
