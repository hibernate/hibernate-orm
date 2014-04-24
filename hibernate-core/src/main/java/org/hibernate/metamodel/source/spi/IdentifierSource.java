/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.spi;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

/**
 * Contract describing source of identifier information for the entity.
 *
 * @author Steve Ebersole
 */
public interface IdentifierSource extends ToolingHintSourceContainer {
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

	/**
	 * Obtain the nature of this identifier source.
	 *
	 * @return The identifier source's nature.
	 */
	public EntityIdentifierNature getNature();

	/**
	 *  Returns the "unsaved" entity identifier value.
	 *
	 *  @todo Not sure this is relevant for anything other than simple identifiers.  Move to SimpleIdentifierSource ?
	 *
	 *  @return the "unsaved" entity identifier value
	 */
	public String getUnsavedValue();
}
