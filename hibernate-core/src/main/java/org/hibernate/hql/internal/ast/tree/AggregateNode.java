/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents an aggregate function i.e. min, max, sum, avg.
 *
 * @author Joshua Davis
 */
public class AggregateNode extends AbstractSelectExpression implements SelectExpression, FunctionNode {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, AggregateNode.class.getName() );

	private SQLFunction sqlFunction;

	public SQLFunction getSQLFunction() {
		return sqlFunction;
	}

	public void resolve() {
		resolveFunction();
	}

	private SQLFunction resolveFunction() {
		if ( sqlFunction == null ) {
			final String name = getText();
			sqlFunction = getSessionFactoryHelper().findSQLFunction( getText() );
			if ( sqlFunction == null ) {
				LOG.unableToResolveAggregateFunction( name );
				sqlFunction = new StandardSQLFunction( name );
			}
		}
		return sqlFunction;
	}

	public Type getFirstArgumentType() {
		AST argument = getFirstChild();
		while ( argument != null ) {
			if ( argument instanceof SqlNode ) {
				final Type type = ( (SqlNode) argument ).getDataType();
				if ( type != null ) {
					return type;
				}
				argument = argument.getNextSibling();
			}
		}
		return null;
	}

	@Override
	public Type getDataType() {
		// Get the function return value type, based on the type of the first argument.
		return getSessionFactoryHelper().findFunctionReturnType( getText(), resolveFunction(), getFirstChild() );
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	@Override
	public boolean isScalar() throws SemanticException {
		// functions in a SELECT should always be considered scalar.
		return true;
	}
}
