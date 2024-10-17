/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

/**
 * The kind of function e.g. normal, aggregate etc.
 *
 * @author Christian Beikov
 */
public enum FunctionKind {
	NORMAL,
	AGGREGATE,
	ORDERED_SET_AGGREGATE,
	WINDOW,
	SET_RETURNING
}
