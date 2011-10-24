/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.hql.internal.ast.tree;

import antlr.Token;
import antlr.collections.AST;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;

/**
 * Base node class for use by Hibernate within its AST trees.
 *
 * @author Joshua Davis
 * @author Steve Ebersole
 */
public class Node extends antlr.CommonAST {
	private String filename;
	private int line;
	private int column;
	private int textLength;

	public Node() {
		super();
	}

	public Node(Token tok) {
		super(tok);  // This will call initialize(tok)!
	}

	/**
	 * Retrieve the text to be used for rendering this particular node.
	 *
	 * @param sessionFactory The session factory
	 * @return The text to use for rendering
	 */
	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		// The basic implementation is to simply use the node's text
		return getText();
	}

	@Override
    public void initialize(Token tok) {
		super.initialize(tok);
		filename = tok.getFilename();
		line = tok.getLine();
		column = tok.getColumn();
		String text = tok.getText();
		textLength = StringHelper.isEmpty(text) ? 0 : text.length();
	}

	@Override
    public void initialize(AST t) {
		super.initialize( t );
		if ( t instanceof Node ) {
			Node n = (Node)t;
			filename = n.filename;
			line = n.line;
			column = n.column;
			textLength = n.textLength;
		}
	}

	public String getFilename() {
		return filename;
	}

	@Override
    public int getLine() {
		return line;
	}

	@Override
    public int getColumn() {
		return column;
	}

	public int getTextLength() {
		return textLength;
	}
}
