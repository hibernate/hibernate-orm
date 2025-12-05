/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;

import java.util.List;

/**
 * Models an auxiliary database object in the boot mapping model
 *
 * @param create The create command
 * @param drop The drop command
 * @param definition (optional) Name of the {@link AuxiliaryDatabaseObject} implementation to use.
 * @param dialectScopes Allows scoping the object to specific dialects.
 *
 * @see AuxiliaryDatabaseObject
 *
 * @author Andrea Boriero
 */
public record DatabaseObjectRegistration(
		String create,
		String drop,
		String definition,
		List<DialectScopeRegistration> dialectScopes ) {
}
