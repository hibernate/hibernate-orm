/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.mapping.Collection;

/**
 * Defines the ability to perform post-creation processing for collection mappings.
 *
 * @apiNote Called after all {@linkplain InFlightEntityMappingType} processing has
 * occurred, allowing access to the runtime mapping model of the entities.
 *
 * @author Steve Ebersole
 */
public interface InFlightCollectionMapping {
	/**
	 * After all hierarchy types have been linked, this method is called to allow
	 * the mapping model to be prepared, which includes creating attribute mapping
	 * descriptors, identifier mapping descriptor, and so on.
	 */
	void prepareMappingModel(MappingModelCreationProcess creationProcess, Collection bootCollectionDescriptor);
}
