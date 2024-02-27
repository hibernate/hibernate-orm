/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.List;

/**
 * Intermediate holder class for results of queries
 * executed using key-based pagination.
 *
 * @author Gavin King
 */
class KeyedResult<R> {
	final R result;
	final List<Comparable<?>> key;

	public KeyedResult(R result, List<Comparable<?>> key) {
		this.result = result;
		this.key = key;
	}

	public R getResult() {
		return result;
	}

	public List<Comparable<?>> getKey() {
		return key;
	}
}
