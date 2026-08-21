/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.dynamic;

import org.hibernate.envers.Audited;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@Audited
@DiscriminatorValue("not null")
public class DefaultNamed extends Named {

	protected DefaultNamed() {
	}

	protected DefaultNamed(String name, String type) {
		super(name, type);
	}
}
