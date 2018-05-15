/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.naming.spi;

import java.util.Collection;
import java.util.UUID;

import org.hibernate.naming.Identifier;
import org.hibernate.naming.NamespaceName;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface RelationalNamespace<T,S> {
	Identifier getCatalogName();

	Identifier getSchemaName();

	Collection<T> getTables();

	Collection<S> getSequences();

	NamespaceName getName();

	T getTable(UUID tableUid);
}
