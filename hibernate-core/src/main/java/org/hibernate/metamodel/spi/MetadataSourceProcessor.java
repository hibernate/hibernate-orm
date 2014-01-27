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
package org.hibernate.metamodel.spi;

import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.IdentifierGeneratorSource;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;

/**
 * Handles the processing of metadata sources in a dependency-ordered manner.
 *
 * @author Steve Ebersole
 */
public interface MetadataSourceProcessor {
	/**
	 * Retrieve the sources pertaining to type descriptors.
	 *
	 * @return The type descriptor sources.
	 */
	Iterable<TypeDescriptorSource> extractTypeDefinitionSources();

	/**
	 * Retrieve the sources pertaining to filter defs.
	 *
	 * @return The filter def sources.
	 */
	Iterable<FilterDefinitionSource> extractFilterDefinitionSources();

	/**
	 * Retrieve the sources of "global" identifier generator specifications.
	 *
	 * @return The identifier generator sources.
	 */
	Iterable<IdentifierGeneratorSource> extractGlobalIdentifierGeneratorSources();

	/**
	 * Retrieve the entity hierarchies.
	 *
	 * @return The entity hierarchies
	 */
	Iterable<EntityHierarchy> extractEntityHierarchies();

	/**
	 * Process the parts of the metadata that depend on mapping (entities, et al) information having been
	 * processed and available.
	 */
	void processMappingDependentMetadata();
}
