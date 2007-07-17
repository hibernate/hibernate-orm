// $Id: CaseNode.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Represents a case ... when .. then ... else ... end expression in a select.
 *
 * @author Gavin King
 */
public class CaseNode extends AbstractSelectExpression implements SelectExpression {
	
	public Type getDataType() {
		return getFirstThenNode().getDataType();
	}

	private SelectExpression getFirstThenNode() {
		return (SelectExpression) getFirstChild().getFirstChild().getNextSibling();
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

}
