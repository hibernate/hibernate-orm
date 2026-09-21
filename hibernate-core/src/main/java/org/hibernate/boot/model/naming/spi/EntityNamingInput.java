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
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record EntityNamingInput(String getClassName, String getEntityName, String getJpaEntityName)
		implements EntityNaming {
	public EntityNamingInput {
		requireNonNull( getEntityName, "entityName" );
	}
}
