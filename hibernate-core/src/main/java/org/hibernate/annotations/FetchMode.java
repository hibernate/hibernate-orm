/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Enumerates the association fetching strategies available in Hibernate.
 * <p>
 * Whereas the JPA {@link jakarta.persistence.FetchType} enumeration provides a way to
 * specify <em>when</em> an association should be fetched, this enumeration provides a
 * way to express <em>how</em> it should be fetched.
 *
 * @author Emmanuel Bernard
 */
public enum FetchMode {
	/**
	 * The association or collection is fetched with a separate subsequent SQL select.
	 */
	SELECT,
	/**
	 * The association or collection is fetched using an outer join clause added to
	 * the initial SQL select.
	 */
	JOIN,
	/**
	 * For collections and many-valued associations only. After the initial SQL select,
	 * all associated collections are fetched together in a single subsequent select.
	 */
	SUBSELECT
}
