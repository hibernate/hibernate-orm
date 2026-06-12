/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.NamedEntityGraph;

/**
 * Factory access to the internal named-graph creator implementations.
 *
 * @author Steve Ebersole
 */
public final class NamedGraphCreators {
	public static NamedGraphCreator jpa(
			NamedEntityGraph annotation,
			String jpaEntityName,
			ModelsContext modelsContext) {
		return new NamedGraphCreatorJpa( annotation, jpaEntityName, java.util.List.of(), modelsContext );
	}

	public static NamedGraphCreator parsed(
			Class<?> entityType,
			org.hibernate.annotations.NamedEntityGraph annotation) {
		return new NamedGraphCreatorParsed( entityType, annotation );
	}

	public static NamedGraphCreator parsed(org.hibernate.annotations.NamedEntityGraph annotation) {
		return new NamedGraphCreatorParsed( annotation );
	}

	private NamedGraphCreators() {
	}
}
