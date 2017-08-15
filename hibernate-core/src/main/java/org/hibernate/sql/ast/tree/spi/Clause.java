/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

/**
 * Used to indicate which query clause we are currently processing
 *
 * @author Steve Ebersole
 */
public enum Clause {
	SELECT,
	FROM,
	WHERE,
	GROUP,
	HAVING,
	ORDER,
	LIMIT
}
