/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.assignment;

import org.hibernate.query.restriction.Path;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 *  * Assignment of a path to a path.
 *
 * @author Gavin King
 */
record PathToPathAssignment<T, X>(Path<T, X> path, Path<T,X> value)
		implements Assignment<T> {
	@Override
	public void apply(SqmUpdateStatement<? extends T> update) {
		update.set( path.path( update.getRoot() ), value.path( update.getRoot() ) );
	}
}
