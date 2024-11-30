/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;

/**
 * Common contract for composite identifiers.  Specific sub-types include aggregated
 * (think {@link jakarta.persistence.EmbeddedId}) and non-aggregated (think
 * {@link jakarta.persistence.IdClass}).
 *
 * @author Steve Ebersole
 */
public interface CompositeIdentifierSource extends IdentifierSource, EmbeddableSourceContributor {
	/**
	 * Handle silly SpecJ reading of the JPA spec.  They believe composite identifiers should have "partial generation"
	 * capabilities.
	 *
	 * @param identifierAttributeName The name of the individual attribute within the composite identifier.
	 *
	 * @return The generator for the named attribute (within the composite).
	 */
	IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName);
}
