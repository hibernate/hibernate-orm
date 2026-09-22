/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.EntityNaming;

import static java.util.Objects.requireNonNull;

/// An immutable snapshot of entity naming facts, with no mapping-model access.
///
/// @param getClassName The fully qualified mapped Java class name, or null when no Java class name is available
/// @param getEntityName The non-null Hibernate entity name used to identify the mapping
/// @param getJpaEntityName The effective Jakarta Persistence entity name, or null when unavailable; this is
/// not necessarily an explicitly supplied [jakarta.persistence.Entity#name()]
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record EntityNamingInput(String getClassName, String getEntityName, String getJpaEntityName)
		implements EntityNaming {
	public EntityNamingInput {
		requireNonNull( getEntityName, "entityName" );
	}
}
