/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
