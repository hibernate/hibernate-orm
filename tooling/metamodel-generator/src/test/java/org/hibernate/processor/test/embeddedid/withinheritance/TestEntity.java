/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddedid.withinheritance;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Immutable
@Table(name = "ENTITY")
public class TestEntity implements Serializable {
	@EmbeddedId
	private Ref ref;

	@Column(name = "NAME", insertable = false, updatable = false, unique = true)
	private String name;
}
