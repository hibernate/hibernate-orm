/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
}
