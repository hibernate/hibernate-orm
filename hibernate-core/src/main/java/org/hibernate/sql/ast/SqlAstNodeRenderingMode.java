/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

/**
 * @author Christian Beikov
 */
public enum SqlAstNodeRenderingMode {
	/**
	 * Render node as is.
	 */
	DEFAULT,

	/**
	 * Render parameters as literals.
	 */
	INLINE_PARAMETERS,

	/**
	 * Don't render plain parameters. Render it as literal or as expression.
	 */
	NO_PLAIN_PARAMETER
}
