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
package org.hibernate.metamodel.source;

import java.util.List;

import org.hibernate.metamodel.MetadataSources;

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
	 * Process the independent metadata.  These have no dependency on other types of metadata being processed.
	 *
	 * @param sources The metadata sources.
	 *
	 * @see #prepare
	 */
	public void processIndependentMetadata(MetadataSources sources);

	/**
	 * Process the parts of the metadata that depend on type information (type definitions) having been processed
	 * and available.
	 *
	 * @param sources The metadata sources.
	 *
	 * @see #processIndependentMetadata
	 */
	public void processTypeDependentMetadata(MetadataSources sources);

	/**
	 * Process the mapping (entities, et al) metadata.
	 *
	 * @param sources The metadata sources.
	 * @param processedEntityNames Collection of any already processed entity names.
	 *
	 * @see #processTypeDependentMetadata
	 */
	public void processMappingMetadata(MetadataSources sources, List<String> processedEntityNames);

	/**
	 * Process the parts of the metadata that depend on mapping (entities, et al) information having been
	 * processed and available.
	 *
	 * @param sources The metadata sources.
	 *
	 * @see #processMappingMetadata
	 */
	public void processMappingDependentMetadata(MetadataSources sources);
}
