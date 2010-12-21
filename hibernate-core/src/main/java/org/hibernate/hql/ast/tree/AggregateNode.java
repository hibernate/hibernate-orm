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
package org.hibernate.hql.ast.tree;

import static org.jboss.logging.Logger.Level.INFO;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.type.Type;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents an aggregate function i.e. min, max, sum, avg.
 *
 * @author Joshua Davis
 */
public class AggregateNode extends AbstractSelectExpression implements SelectExpression, FunctionNode {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                AggregateNode.class.getPackage().getName());

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
                LOG.unableToResolveAggregateFunction(name);
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

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = INFO )
        @Message( value = "Could not resolve aggregate function {}; using standard definition" )
        void unableToResolveAggregateFunction( String name );
    }
}
