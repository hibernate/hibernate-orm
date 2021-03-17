/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.Incubating;

/**
 * Used to indicate which query clause we are currently processing
 *
 * @author Steve Ebersole
 */
@Incubating
public enum Clause {
	/**
	 * The insert values clause
	 */
	INSERT,

	/**
	 * The update set clause
	 */
	UPDATE,

	/**
	 * Not used in 5.x.  Intended for use in 6+ as indicator
	 * of processing predicates (where clause) that occur in a
	 * delete
	 */
	DELETE,

	SELECT,
	FROM,
	WHERE,
	GROUP,
	HAVING,
	ORDER,
	LIMIT,
	CALL,

	/**
	 * Again, not used in 5.x.  Used in 6+
	 */
	IRRELEVANT

}
