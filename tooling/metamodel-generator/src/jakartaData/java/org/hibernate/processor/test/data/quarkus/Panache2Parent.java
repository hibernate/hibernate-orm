/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.quarkus;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

@Entity
@Inheritance
public class Panache2Parent implements PanacheEntityMarker {
	@Id
	public Long id;
}
