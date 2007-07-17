package org.hibernate.hql.ast.tree;

import antlr.collections.AST;
import antlr.Token;
import org.hibernate.util.StringHelper;
import org.hibernate.engine.SessionFactoryImplementor;

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

	public void initialize(Token tok) {
		super.initialize(tok);
		filename = tok.getFilename();
		line = tok.getLine();
		column = tok.getColumn();
		String text = tok.getText();
		textLength = StringHelper.isEmpty(text) ? 0 : text.length();
	}

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

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public int getTextLength() {
		return textLength;
	}
}
