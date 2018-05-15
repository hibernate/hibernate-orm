/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.naming;

/**
 * @author Steve Ebersole
 */
public class QualifiedSequenceName extends QualifiedNameImpl {
	public QualifiedSequenceName(Identifier catalogName, Identifier schemaName, Identifier sequenceName) {
		super( catalogName, schemaName, sequenceName );
	}

	public QualifiedSequenceName(NamespaceName namespaceName, Identifier sequenceName) {
		super( namespaceName, sequenceName );
	}

	public Identifier getSequenceName() {
		return getObjectName();
	}
}
