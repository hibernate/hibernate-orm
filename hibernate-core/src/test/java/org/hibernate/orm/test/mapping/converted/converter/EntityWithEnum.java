/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Jan Schatteman
 */
@Entity(name = "EntityWithEnum")
public class EntityWithEnum {
	@Id
	private Long id;
	@Convert(converter = AttributeConverterAndFunctionTest.StateJpaConverter.class)
	private AttributeConverterAndFunctionTest.State state;

	public EntityWithEnum() {
	}

	public EntityWithEnum(Long id, AttributeConverterAndFunctionTest.State state) {
		this.id = id;
		this.state = state;
	}
}
