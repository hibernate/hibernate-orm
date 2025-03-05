/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
class Town {
	@EmbeddedId
	@AttributeOverride(name = "countryCode", column=@Column(name="town_country_code"))
	@AttributeOverride(name = "zipCode", column=@Column(name="town_zip_code"))
	TownCode townCode;

	@ManyToOne
	@JoinColumn(name = "town_zip_code", referencedColumnName = "region_zip_code", insertable = false, updatable = false)
	@JoinColumn(name = "town_country_code", referencedColumnName = "region_country_code", insertable = false, updatable = false)
	Region region;
}
