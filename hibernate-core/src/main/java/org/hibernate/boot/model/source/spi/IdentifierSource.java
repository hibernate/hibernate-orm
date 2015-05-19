/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public EntityIdentifierNature getNature();

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
