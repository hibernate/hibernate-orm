/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.namespace.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.namespace.internal.StandardNamespaceSupport;

import static org.hibernate.SPI.Role.USE;

/// Supplies immutable stock catalog and schema lifecycle strategies.
///
/// Use [#standard()] for ordinary schema DDL, the Boolean overload when the
/// schema grammar admits existence clauses, [#none()] when neither namespace
/// kind is manageable, and [#catalogsAsDatabases()] for MySQL-style database
/// catalogs.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class NamespaceSupports {
	private static final NamespaceSupport NONE = new StandardNamespaceSupport(
			false,
			NamespaceSupports::unsupportedCatalog,
			NamespaceSupports::unsupportedCatalog,
			false,
			NamespaceSupports::unsupportedSchema,
			NamespaceSupports::unsupportedSchema
	);
	private static final NamespaceSupport CATALOGS_AS_DATABASES = new StandardNamespaceSupport(
			true,
			name -> new String[] { "create database " + name },
			name -> new String[] { "drop database " + name },
			false,
			NamespaceSupports::unsupportedSchema,
			NamespaceSupports::unsupportedSchema
	);
	private static final NamespaceSupport[] STANDARD = {
			standardProfile( false, false ),
			standardProfile( false, true ),
			standardProfile( true, false ),
			standardProfile( true, true )
	};

	private NamespaceSupports() {
	}

	/// Return ordinary schema lifecycle support without existence clauses.
	public static NamespaceSupport standard() {
		return STANDARD[0];
	}

	/// Return ordinary schema lifecycle support with the requested existence
	/// clauses.
	public static NamespaceSupport standard(
			boolean createSchemaIfNotExists,
			boolean dropSchemaIfExists) {
		return STANDARD[(createSchemaIfNotExists ? 2 : 0) + (dropSchemaIfExists ? 1 : 0)];
	}

	/// Return the profile which manages neither catalogs nor schemas.
	public static NamespaceSupport none() {
		return NONE;
	}

	/// Return the profile which manages catalogs with `create database` and
	/// `drop database` while disabling schema lifecycle operations.
	public static NamespaceSupport catalogsAsDatabases() {
		return CATALOGS_AS_DATABASES;
	}

	private static NamespaceSupport standardProfile(
			boolean createSchemaIfNotExists,
			boolean dropSchemaIfExists) {
		return new StandardNamespaceSupport(
				false,
				NamespaceSupports::unsupportedCatalog,
				NamespaceSupports::unsupportedCatalog,
				true,
				name -> new String[] {
						(createSchemaIfNotExists ? "create schema if not exists " : "create schema ") + name
				},
				name -> new String[] {
						(dropSchemaIfExists ? "drop schema if exists " : "drop schema ") + name
				}
		);
	}

	private static String[] unsupportedCatalog(String name) {
		throw new UnsupportedOperationException( "Catalog lifecycle is not supported" );
	}

	private static String[] unsupportedSchema(String name) {
		throw new UnsupportedOperationException( "Schema lifecycle is not supported" );
	}
}
