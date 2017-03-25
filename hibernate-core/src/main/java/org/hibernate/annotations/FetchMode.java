/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Fetch options on associations.  Defines more of the "how" of fetching, whereas JPA {@link javax.persistence.FetchType}
 * focuses on the "when".
 *
 * @author Emmanuel Bernard
 */
public enum FetchMode {
	/**
	 * Use a secondary select for each individual entity, collection, or join load.
	 */
	SELECT,
	/**
	 * Use an outer join to load the related entities, collections or joins.
	 */
	JOIN,
	/**
	 * Available for collections only.  When accessing a non-initialized collection, this fetch mode will trigger loading all elements of all collections of the same role for all owners associated with the persistence context using a single secondary select.
	 */
	SUBSELECT
}
