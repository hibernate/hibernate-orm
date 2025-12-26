/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;


import java.util.Map;

/// Used to determine whether a constraint (index, unique key, etc.)
/// should be validated.
///
/// @implNote Yes, yes, an index is not technically a constraint - this is just
/// for nice simple naming.
///
/// @see org.hibernate.cfg.SchemaToolingSettings#INDEX_VALIDATION
/// @see org.hibernate.cfg.SchemaToolingSettings#UNIQUE_KEY_VALIDATION
///
/// @since 7.3
///
/// @author Steve Ebersole
public enum ConstraintValidationType {
	/// No validation will occur.
	NONE,
	/// Validation will occur only for explicitly named constraints.
	NAMED,
	/// Validation will occur for all constraints.
	ALL;

	public static ConstraintValidationType interpret(
			String name,
			Map<String, Object> configurationValues) {
		final Object setting = configurationValues.get( name );
		if ( setting == null ) {
			return NONE;
		}

		if ( setting instanceof ConstraintValidationType type ) {
			return type;
		}

		var settingName = setting.toString();
		if ( NAMED.name().equalsIgnoreCase( settingName ) ) {
			return NAMED;
		}
		else if ( ALL.name().equalsIgnoreCase( settingName ) ) {
			return ALL;
		}
		else {
			return NONE;
		}
	}
}
