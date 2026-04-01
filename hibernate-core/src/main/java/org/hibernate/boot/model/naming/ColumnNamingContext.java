/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.internal.AnnotatedColumns;
import org.hibernate.boot.model.internal.PropertyHolder;

/**
 * Column context passed with column into naming strategies
 * for end user to implement their custom logic more easily.
 *
 * @author Vasya Petrov
 */
public record ColumnNamingContext(String entityName, String className) {
	public ColumnNamingContext(AnnotatedColumns annotatedColumns) {
		this( annotatedColumns == null ? null : annotatedColumns.getPropertyHolder() );
	}

	public ColumnNamingContext(PropertyHolder propertyHolder) {
		this(
				extractEntityName( propertyHolder ),
				extractClassName( propertyHolder )
		);
	}

	private static String extractEntityName(PropertyHolder propertyHolder) {
		return propertyHolder == null
				? null
				: propertyHolder.getEntityName();
	}

	private static String extractClassName(PropertyHolder propertyHolder) {
		return propertyHolder == null
				? null
				: propertyHolder.getEntityOwnerClassName();
	}
}
