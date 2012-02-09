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

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.FilterDefSource;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;

/**
 * Handles the processing of metadata sources in a dependency-ordered manner.
 *
 * @author Steve Ebersole
 */
public interface MetadataSourceProcessor {
	/**
	 * Prepare for processing the given sources.
	 *
	 * @param sources The metadata sources.
	 */
	public void prepare(MetadataSources sources);

	/**
	 * Retrieve the sources pertaining to type descriptors.
	 *
	 * @param sources The metadata sources.
	 *
	 * @return The type descriptor sources.
	 */
	public Iterable<? extends TypeDescriptorSource> extractTypeDescriptorSources(MetadataSources sources);

	/**
	 * Retrieve the sources pertaining to filter defs.
	 *
	 * @param sources The metadata sources.
	 *
	 * @return The filter def sources.
	 */
	public Iterable<? extends FilterDefSource> extractFilterDefSources(MetadataSources sources);

	/**
	 * Retrieve the entity hierarchies.
	 *
	 * @param sources The metadata sources.
	 *
	 * @return The entity hierarchies
	 */
	public Iterable<? extends EntityHierarchy> extractEntityHierarchies(MetadataSources sources);

	/**
	 * Process the parts of the metadata that depend on mapping (entities, et al) information having been
	 * processed and available.
	 *
	 * @param sources The metadata sources.
	 */
	public void processMappingDependentMetadata(MetadataSources sources);
}
