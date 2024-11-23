/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
