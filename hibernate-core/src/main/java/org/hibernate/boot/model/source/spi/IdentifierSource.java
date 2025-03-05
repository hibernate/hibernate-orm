/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.id.EntityIdentifierNature;

/**
 * Contract describing source of identifier information for the entity.
 *
 * @author Steve Ebersole
 */
public interface IdentifierSource extends ToolingHintContextContainer {
	/**
	 * Obtain the nature of this identifier source.
	 *
	 * @return The identifier source's nature.
	 */
	EntityIdentifierNature getNature();

	/**
	 * Obtain the identifier generator source.
	 *
	 * @todo this should name a source as well, no?
	 * 		Basically, not sure it should be up to the sources to build binding objects.
	 * 		IdentifierGeneratorSource, possibly as a hierarchy as well to account for differences
	 * 		in "global" versus "local" declarations
	 *
	 * @return The generator source.
	 */
	IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor();
}
