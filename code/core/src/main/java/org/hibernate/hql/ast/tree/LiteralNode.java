// $Id: LiteralNode.java 10060 2006-06-28 02:53:39Z steve.ebersole@jboss.com $
package org.hibernate.hql.ast.tree;

import org.hibernate.Hibernate;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Represents a literal.
 *
 * @author josh Jan 8, 2005 10:09:53 AM
 */
public class LiteralNode extends AbstractSelectExpression implements HqlSqlTokenTypes {

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	public Type getDataType() {
		switch ( getType() ) {
			case NUM_INT:
				return Hibernate.INTEGER;
			case NUM_FLOAT:
				return Hibernate.FLOAT;
			case NUM_LONG:
				return Hibernate.LONG;
			case NUM_DOUBLE:
				return Hibernate.DOUBLE;
			case QUOTED_STRING:
				return Hibernate.STRING;
			case TRUE:
			case FALSE:
				return Hibernate.BOOLEAN;
			default:
				return null;
		}
	}
}
