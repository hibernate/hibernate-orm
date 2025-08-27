/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.discriminatoroptions;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.DiscriminatorOptions;

/**
 * @author Hardy Ferentschik
 */
@Entity
@DiscriminatorValue("B")
@DiscriminatorOptions(force = true, insert = false)
public class BaseClass {
	@Id
	@GeneratedValue
	private long id;
}
