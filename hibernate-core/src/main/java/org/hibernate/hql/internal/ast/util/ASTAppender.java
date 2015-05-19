/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.util;
import antlr.ASTFactory;
import antlr.collections.AST;

/**
 * Appends child nodes to a parent efficiently.
 *
 * @author Joshua Davis
 */
public class ASTAppender {
	private AST parent;
	private AST last;
	private ASTFactory factory;

	/**
	 * Builds an appender using the given factory and parent
	 *
	 * @param factory The AST factory
	 * @param parent The AST parent
	 */
	public ASTAppender(ASTFactory factory, AST parent) {
		this.factory = factory;
		this.parent = parent;
		this.last = ASTUtil.getLastChild( parent );
	}

	/**
	 * Append a new child to parent using the given node type and text, but only if the
	 * text is non-empty
	 *
	 * @param type The node type
	 * @param text The node text
	 * @param appendIfEmpty Should we do the append if the text is empty?
	 *
	 * @return The generated AST node; may return {@code null}
	 */
	public AST append(int type, String text, boolean appendIfEmpty) {
		if ( text != null && ( appendIfEmpty || text.length() > 0 ) ) {
			return append( factory.create( type, text ) );
		}
		else {
			return null;
		}
	}

	/**
	 * Append the given AST node as a child of parent.
	 *
	 * @param child The child to append
	 *
	 * @return Returns what was passed in.
	 */
	public AST append(AST child) {
		if ( last == null ) {
			parent.setFirstChild( child );
		}
		else {
			last.setNextSibling( child );
		}
		last = child;
		return last;
	}
}
