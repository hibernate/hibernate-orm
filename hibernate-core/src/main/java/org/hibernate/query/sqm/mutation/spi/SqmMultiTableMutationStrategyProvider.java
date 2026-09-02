/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.spi;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.service.Service;

/**
 * Pluggable contract for providing custom {@link SqmMultiTableMutationStrategy} and
 * {@link SqmMultiTableInsertStrategy} implementations. This is intended for use by
 * hibernate-reactive to provide its custom implementations.
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface SqmMultiTableMutationStrategyProvider extends Service {
	/// Determines the multi-table mutation strategy for the given entity.
	SqmMultiTableMutationStrategy createMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext creationContext);

	/// Determines the multi-table insert strategy for the given entity.
	SqmMultiTableInsertStrategy createInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext creationContext);
}
