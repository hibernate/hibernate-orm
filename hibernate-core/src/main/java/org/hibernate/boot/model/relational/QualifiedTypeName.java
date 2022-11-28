/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
