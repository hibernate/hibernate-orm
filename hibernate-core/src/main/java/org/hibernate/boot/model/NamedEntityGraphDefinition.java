/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import jakarta.persistence.NamedEntityGraph;

import java.util.Objects;

/**
 * Models a {@linkplain NamedEntityGraph @NamedEntityGraph}
 *
 * @author Steve Ebersole
 */
public record NamedEntityGraphDefinition
		(String name, String entityName, Source source, NamedGraphCreator graphCreator) {
	public enum Source { JPA, PARSED }

	public NamedEntityGraphDefinition {
		Objects.requireNonNull( name, "Named entity graph name cannot be null" );
	}

	@Deprecated(since = "7.0", forRemoval = true)
	public String getRegisteredName() {
		return name;
	}

	@Deprecated(since = "7.0", forRemoval = true)
	public String getEntityName() {
		return entityName;
	}
}
