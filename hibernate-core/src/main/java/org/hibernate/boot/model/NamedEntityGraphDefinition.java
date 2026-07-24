/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import java.io.Serializable;
import jakarta.persistence.NamedEntityGraph;

import java.util.Objects;

/**
 * Models a {@linkplain NamedEntityGraph @NamedEntityGraph}
 *
 * @author Steve Ebersole
 */
public record NamedEntityGraphDefinition
		(String name, String entityName, Source source, NamedGraphCreator graphCreator) implements Serializable {
	public enum Source { JPA, PARSED }

	public NamedEntityGraphDefinition {
		Objects.requireNonNull( name, "Named entity graph name cannot be null" );
	}
}
