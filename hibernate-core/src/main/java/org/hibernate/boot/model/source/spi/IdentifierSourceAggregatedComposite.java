/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

/**
 * Additional contract describing the source of an identifier mapping whose
 * {@linkplain #getNature() nature} is
 * {@link org.hibernate.id.EntityIdentifierNature#AGGREGATED_COMPOSITE}.
 * <p>
 * This equates to an identifier which is made up of multiple values which are
 * defined as part of a component/embedded; i.e. {@link jakarta.persistence.EmbeddedId}
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface IdentifierSourceAggregatedComposite extends CompositeIdentifierSource {
	/**
	 * Obtain the source descriptor for the identifier attribute.
	 *
	 * @return The identifier attribute source.
	 */
	SingularAttributeSourceEmbedded getIdentifierAttributeSource();

	/**
	 * Obtain the mapping of attributes annotated with {@link jakarta.persistence.MapsId}.
	 *
	 * @return The MapsId sources.
	 */
	List<MapsIdSource> getMapsIdSources();
}
