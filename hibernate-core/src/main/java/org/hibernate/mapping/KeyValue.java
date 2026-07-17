/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * A mapping model {@link Value} which may be treated as an identifying key of a
 * relational database table. A {@code KeyValue} might represent the primary key
 * of an entity or the foreign key of a collection, join table, secondary table,
 * or joined subclass table.
 *
 * @author Gavin King
 */
public interface KeyValue extends Value {

	boolean isCascadeDeleteEnabled();

	enum NullValueSemantic { VALUE, NULL, NEGATIVE, UNDEFINED, NONE, ANY }

	NullValueSemantic getNullValueSemantic();

	String getNullValue();

	boolean isUpdateable();
}
