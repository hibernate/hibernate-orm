/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contract for things that can contain EmbeddableSource definitions.
 *
 * @author Steve Ebersole
 */
public interface EmbeddableSourceContributor {
	/**
	 * Gets the source information about the embeddable/composition.
	 *
	 * @return The EmbeddableSource
	 */
	EmbeddableSource getEmbeddableSource();
}
