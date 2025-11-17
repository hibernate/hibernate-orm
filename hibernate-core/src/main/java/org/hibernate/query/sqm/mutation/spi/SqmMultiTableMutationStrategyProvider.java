/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.spi;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.service.Service;

/**
 * Pluggable contract for providing custom {@link SqmMultiTableMutationStrategy} and
 * {@link SqmMultiTableInsertStrategy} implementations. This is intended for use by
 * hibernate-reactive to provide its custom implementations.
 *
 * @author Steve Ebersole
 */
public interface SqmMultiTableMutationStrategyProvider extends Service {
	/**
	 * Determine the SqmMultiTableMutationStrategy to use for the given entity
	 */
	SqmMultiTableMutationStrategy createMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			MappingModelCreationProcess creationProcess);

	/**
	 * Determine the SqmMultiTableInsertStrategy to use for the given entity
	 */
	SqmMultiTableInsertStrategy createInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			MappingModelCreationProcess creationProcess);
}
