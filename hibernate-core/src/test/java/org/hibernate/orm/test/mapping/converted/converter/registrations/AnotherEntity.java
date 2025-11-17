/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import org.hibernate.annotations.ConverterRegistration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity demonstrating the registration of a converter for a specific type, other
 * than what the converter advertises
 *
 * @author Steve Ebersole
 */
@Entity
@ConverterRegistration( converter = Thing1Converter.class, domainType = Thing.class )
public class AnotherEntity {
	@Id
	private Integer id;

	private String name;
	private Thing thing;

	private AnotherEntity() {
		// for Hibernate use
	}

	public AnotherEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
