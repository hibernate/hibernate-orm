/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.mixed;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.NaturalId;

@Entity
class Town {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false)
	Integer id;

	String name;

	@NaturalId
	@Embedded
	TownCode townCode;

	@Column(name = "region_id", nullable = false)
	int regionId;

	@ManyToOne
	@JoinColumn(name = "region_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
	@JoinColumn(name = "country_code", referencedColumnName = "country_code", nullable = false, insertable = false, updatable = false)
	@JoinColumn(name = "zip_code", referencedColumnName = "zip_code", nullable = false, insertable = false, updatable = false)
	Region region;
}
