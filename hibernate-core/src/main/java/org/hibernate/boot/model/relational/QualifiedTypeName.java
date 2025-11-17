/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Christian Beikov
 */
public class QualifiedTypeName extends QualifiedNameImpl {
	public QualifiedTypeName(Identifier catalogName, Identifier schemaName, Identifier tableName) {
		super( catalogName, schemaName, tableName );
	}

	public QualifiedTypeName(Namespace.Name schemaName, Identifier tableName) {
		super( schemaName, tableName );
	}

	public Identifier getTypeName() {
		return getObjectName();
	}

	public QualifiedTypeName quote() {
		Identifier catalogName = getCatalogName();
		if ( catalogName != null ) {
			catalogName = new Identifier( catalogName.getText(), true );
		}
		Identifier schemaName = getSchemaName();
		if ( schemaName != null ) {
			schemaName = new Identifier( schemaName.getText(), true );
		}
		Identifier tableName = getTypeName();
		if ( tableName != null ) {
			tableName = new Identifier( tableName.getText(), true );
		}
		return new QualifiedTypeName( catalogName, schemaName, tableName );
	}
}
