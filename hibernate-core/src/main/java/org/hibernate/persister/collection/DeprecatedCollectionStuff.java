/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.collection;

/**
 * @deprecated Just used to singly extend all the deprecated collection persister roles
 */
@Deprecated
public interface DeprecatedCollectionStuff extends SQLLoadableCollection {
	@Override
	default String getRole() {
		return SQLLoadableCollection.super.getRole();
	}
}
