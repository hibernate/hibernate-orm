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
	 * use a secondary select for each individual entity, collection, or join load.
	 */
	SELECT,
	/**
	 * use an outer join to load the related entities, collections or joins.
	 */
	JOIN,
	/**
	 * use a secondary select so that, for a given collection, it can load all child entities associated with every parent table record using a single secondary select statement.
	 */
	SUBSELECT
}
