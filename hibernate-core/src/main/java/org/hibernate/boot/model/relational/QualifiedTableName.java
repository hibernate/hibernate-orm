/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public class QualifiedTableName extends QualifiedNameImpl {
	public QualifiedTableName(Identifier catalogName, Identifier schemaName, Identifier tableName) {
		super( catalogName, schemaName, tableName );
	}

	public QualifiedTableName(Namespace.Name schemaName, Identifier tableName) {
		super( schemaName, tableName );
	}

	public Identifier getTableName() {
		return getObjectName();
	}

	public QualifiedTableName quote() {
		Identifier catalogName = getCatalogName();
		if ( catalogName != null ) {
			catalogName = new Identifier( catalogName.getText(), true );
		}
		Identifier schemaName = getSchemaName();
		if ( schemaName != null ) {
			schemaName = new Identifier( schemaName.getText(), true );
		}
		Identifier tableName = getTableName();
		if ( tableName != null ) {
			tableName = new Identifier( tableName.getText(), true );
		}
		return new QualifiedTableName( catalogName, schemaName, tableName );
	}
}
