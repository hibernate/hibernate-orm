/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
