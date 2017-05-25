/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.naming;

import org.hibernate.naming.spi.QualifiedName;
import org.hibernate.naming.spi.QualifiedNameParser;

/**
 * @author Steve Ebersole
 */
public class QualifiedNameImpl extends QualifiedNameParser.NameParts implements QualifiedName {
	public QualifiedNameImpl(NamespaceName namespaceName, Identifier objectName) {
		this(
				namespaceName.getCatalog(),
				namespaceName.getSchema(),
				objectName
		);
	}

	public QualifiedNameImpl(Identifier catalogName, Identifier schemaName, Identifier objectName) {
		super( catalogName, schemaName, objectName );
	}
}
