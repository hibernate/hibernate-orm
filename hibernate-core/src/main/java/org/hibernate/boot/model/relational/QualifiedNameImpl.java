/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public class QualifiedNameImpl extends QualifiedNameParser.NameParts implements QualifiedName {
	public QualifiedNameImpl(Namespace.Name schemaName, Identifier objectName) {
		this( schemaName.catalog(), schemaName.schema(), objectName );
	}

	public QualifiedNameImpl(Identifier catalogName, Identifier schemaName, Identifier objectName) {
		super( catalogName, schemaName, objectName );
	}
}
