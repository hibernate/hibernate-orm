/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

/// Naming capabilities for the mapping currently being interpreted.
/// Hibernate supplies this context to implicit naming strategies.
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public interface ImplicitNamingContext {
	/// Construct an implicit logical name from text without quote delimiters.
	default LogicalName implicitName(String text) {
		return implicitName( text, false );
	}

	/// Construct an implicit logical name with requested quoting. Physical
	/// finalization applies global and automatic quoting afterward.
	default LogicalName implicitName(String text, boolean quoted) {
		return new LogicalName( text, quoted, false );
	}

	/// The effective naming defaults for this mapping.
	ImplicitNamingDefaults getNamingDefaults();

	/// The configured helper for identifier conversion and quoting.
	IdentifierHelper getIdentifierHelper();

	/// The charset for hashing generated constraint names, or null for the default.
	String getSchemaCharset();
}
