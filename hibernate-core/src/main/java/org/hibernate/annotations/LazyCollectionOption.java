/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Lazy options available for a collection.
 *
 * @author Emmanuel Bernard
 */
public enum LazyCollectionOption {
	/**
	 * Eagerly load it.
	 */
	FALSE,
	/**
	 * Load it when the state is requested.
	 */
	TRUE,
	/**
	 * Prefer extra queries over full collection loading.
	 */
	EXTRA
}
