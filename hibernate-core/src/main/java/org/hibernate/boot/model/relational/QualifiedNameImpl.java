/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public class QualifiedNameImpl extends QualifiedNameParser.NameParts implements QualifiedName {
	public QualifiedNameImpl(Namespace.Name schemaName, Identifier objectName) {
		this( schemaName.getCatalog(), schemaName.getSchema(), objectName );
	}

	public QualifiedNameImpl(Identifier catalogName, Identifier schemaName, Identifier objectName) {
		super( catalogName, schemaName, objectName );
	}
}
