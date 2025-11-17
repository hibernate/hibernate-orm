/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Mainly this is used to support legacy sequence exporting.
 */
public class NamedAuxiliaryDatabaseObject
		extends SimpleAuxiliaryDatabaseObject
		implements Exportable {
	private final String name;

	public NamedAuxiliaryDatabaseObject(
			String name,
			Namespace namespace,
			String[] createStrings,
			String[] dropStrings,
			Set<String> dialectScopes) {
		super( namespace, createStrings, dropStrings, dialectScopes );
		this.name = name;
	}

	public NamedAuxiliaryDatabaseObject(
			String name,
			Namespace namespace,
			String[] createStrings,
			String[] dropStrings,
			Set<String> dialectScopes,
			boolean beforeTables) {
		super( namespace, createStrings, dropStrings, dialectScopes, beforeTables );
		this.name = name;
	}

	public NamedAuxiliaryDatabaseObject(
			String name,
			Namespace namespace,
			String createString,
			String dropString,
			Set<String> dialectScopes) {
		super( namespace, createString, dropString, dialectScopes );
		this.name = name;
	}

	public NamedAuxiliaryDatabaseObject(
			String name,
			Namespace namespace,
			String createString,
			String dropString,
			Set<String> dialectScopes,
			boolean beforeTables) {
		super( namespace, createString, dropString, dialectScopes, beforeTables );
		this.name = name;
	}

	@Override
	public String getExportIdentifier() {
		return new QualifiedNameImpl(
				Identifier.toIdentifier( getCatalogName() ),
				Identifier.toIdentifier( getSchemaName() ),
				Identifier.toIdentifier( name )
		).render();
	}
}
