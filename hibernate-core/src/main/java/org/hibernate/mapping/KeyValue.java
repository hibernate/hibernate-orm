/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;

/**
 * A mapping model {@link Value} which may be treated as an identifying key of a
 * relational database table. A {@code KeyValue} might represent the primary key
 * of an entity or the foreign key of a collection, join table, secondary table,
 * or joined subclass table.
 *
 * @author Gavin King
 */
public interface KeyValue extends Value {

	ForeignKey createForeignKeyOfEntity(String entityName);

	boolean isCascadeDeleteEnabled();

	String getNullValue();

	boolean isUpdateable();

	@Deprecated(since = "7.0")
	default Generator createGenerator(Dialect dialect, RootClass rootClass) {
		return createGenerator( dialect, rootClass, null );
	}

	Generator createGenerator(Dialect dialect, RootClass rootClass, Property property);

}
