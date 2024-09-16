/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * The insert clause
	 */
	INSERT,

	/**
	 * The values clause
	 */
	VALUES,

	/**
	 * The update clause
	 */
	UPDATE,

	/**
	 * The update set clause
	 */
	SET,

	/**
	 * The update set clause expression part
	 */
	SET_EXPRESSION,

	/**
	 * Not used in 5.x.  Intended for use in 6+ as indicator
	 * of processing predicates (where clause) that occur in a
	 * delete
	 */
	DELETE,
	MERGE,

	SELECT,
	FROM,
	WHERE,
	GROUP,
	HAVING,
	ORDER,
	OFFSET,
	FETCH,
	OVER,
	/**
	 * The clause containing CTEs
	 */
	WITH,
	WITHIN_GROUP,
	PARTITION,
	CONFLICT,
	CALL,

	/**
	 * Again, not used in 5.x.  Used in 6+
	 */
	IRRELEVANT

}
