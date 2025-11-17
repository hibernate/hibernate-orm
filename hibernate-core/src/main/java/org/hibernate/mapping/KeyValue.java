/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;

import java.util.List;

/**
 * A mapping model {@link Value} which may be treated as an identifying key of a
 * relational database table. A {@code KeyValue} might represent the primary key
 * of an entity or the foreign key of a collection, join table, secondary table,
 * or joined subclass table.
 *
 * @author Gavin King
 */
public interface KeyValue extends Value {

	ForeignKey createForeignKeyOfEntity(String entityName, List<Column> referencedColumns);

	ForeignKey createForeignKeyOfEntity(String entityName);

	boolean isCascadeDeleteEnabled();

	enum NullValueSemantic { VALUE, NULL, NEGATIVE, UNDEFINED, NONE, ANY }

	NullValueSemantic getNullValueSemantic();

	String getNullValue();

	boolean isUpdateable();

	/**
	 * @deprecated No longer called, except from tests.
	 *             Use {@link #createGenerator(Dialect, RootClass, Property, GeneratorSettings)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	Generator createGenerator(Dialect dialect, RootClass rootClass);

	@Incubating
	Generator createGenerator(Dialect dialect, RootClass rootClass, Property property, GeneratorSettings defaults);
}
