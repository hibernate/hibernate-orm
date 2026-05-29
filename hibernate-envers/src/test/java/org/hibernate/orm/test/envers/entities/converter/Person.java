/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.converter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.envers.Audited;

/**
 * @author Steve Ebersole
 */
@Entity
@Audited
public class Person {
	@Id
	@GenericGenerator(type = IncrementGenerator.class )
	private Long id;

	@Convert(converter = SexConverter.class)
	private Sex sex;
}
