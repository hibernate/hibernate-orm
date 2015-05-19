/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

/**
 * General contract for AST nodes.
 *
 * @author Steve Ebersole
 */
public interface Node {
	/**
	 * Get the intrinsic text of this node.
	 *
	 * @return The node's text.
	 */
	public String getText();

	/**
	 * Get a string representation of this node usable for debug logging or similar.
	 *
	 * @return The node's debugging text.
	 */
	public String getDebugText();

	/**
	 * Build the node's representation for use in the resulting rendering.
	 *
	 * @return The text for use in the translated output.
	 */
	public String getRenderableText();
}
