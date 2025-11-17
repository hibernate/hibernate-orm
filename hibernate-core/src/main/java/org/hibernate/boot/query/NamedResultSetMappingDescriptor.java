/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.spi.QueryEngine;

/**
 * Models the "boot view" of a ResultSet mapping used in the mapping
 * of native and procedure queries.
 *
 * Ultimately used to generate a NamedResultSetMappingMemento that is
 * stored in the {@link NamedObjectRepository}
 * for availability at runtime
 *
 * @author Steve Ebersole
 */
public interface NamedResultSetMappingDescriptor {
	/**
	 * The name under which the result-set-mapping is to be registered
	 */
	String getRegistrationName();

	/**
	 * Create a representation of the described ResultSet mapping for the purpose of
	 * being stored in Hibernate's {@link NamedObjectRepository}
	 *
	 * @see QueryEngine#getNamedObjectRepository()
	 */
	NamedResultSetMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext);
}
