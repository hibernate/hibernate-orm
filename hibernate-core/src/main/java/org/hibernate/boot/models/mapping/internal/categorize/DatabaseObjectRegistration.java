/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import java.util.List;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;

/// Models an auxiliary database object in the boot mapping model.
///
/// @param create The create command
/// @param drop The drop command
/// @param definition Optional name of the [AuxiliaryDatabaseObject] implementation to use
/// @param dialectScopes Dialect-specific scoping for the object
///
/// @see AuxiliaryDatabaseObject
///
/// @since 9.0
/// @author Steve Ebersole
public record DatabaseObjectRegistration(
		String create,
		String drop,
		String definition,
		List<DialectScopeRegistration> dialectScopes) {

	public record DialectScopeRegistration(
			String name,
			String content,
			String minimumVersion,
			String maximumVersion) {
	}
}
