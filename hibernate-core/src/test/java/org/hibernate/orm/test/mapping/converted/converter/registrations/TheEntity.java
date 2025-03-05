/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import org.hibernate.annotations.ConverterRegistration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity demonstrating using registrations which enable auto-apply
 *
 * @author Steve Ebersole
 */
@Entity
@ConverterRegistration( converter = Thing1Converter.class, autoApply = true )
@ConverterRegistration( converter = Thing2Converter.class, autoApply = true )
public class TheEntity {
	@Id
	private Integer id;

	private String name;
	private Thing1 thing1;
	private Thing2 thing2;


	private TheEntity() {
		// for Hibernate use
	}

	public TheEntity(Integer id, String name) {
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

	public Thing1 getThing1() {
		return thing1;
	}

	public void setThing1(Thing1 thing1) {
		this.thing1 = thing1;
	}

	public Thing2 getThing2() {
		return thing2;
	}

	public void setThing2(Thing2 thing2) {
		this.thing2 = thing2;
	}
}
