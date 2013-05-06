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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * A function which takes no arguments
 *
 * @author Michi
 */
public class NoArgSQLFunction implements SQLFunction {
	private Type returnType;
	private boolean hasParenthesesIfNoArguments;
	private String name;

	/**
	 * Constructs a NoArgSQLFunction
	 *
	 * @param name The function name
	 * @param returnType The function return type
	 */
	public NoArgSQLFunction(String name, Type returnType) {
		this( name, returnType, true );
	}

	/**
	 * Constructs a NoArgSQLFunction
	 *
	 * @param name The function name
	 * @param returnType The function return type
	 * @param hasParenthesesIfNoArguments Does the function call need parenthesis if there are no arguments?
	 */
	public NoArgSQLFunction(String name, Type returnType, boolean hasParenthesesIfNoArguments) {
		this.returnType = returnType;
		this.hasParenthesesIfNoArguments = hasParenthesesIfNoArguments;
		this.name = name;
	}

	@Override
	public boolean hasArguments() {
		return false;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return hasParenthesesIfNoArguments;
	}

	@Override
	public Type getReturnType(Type argumentType, Mapping mapping) throws QueryException {
		return returnType;
	}

	@Override
	public String render(Type argumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		if ( args.size() > 0 ) {
			throw new QueryException( "function takes no arguments: " + name );
		}
		return hasParenthesesIfNoArguments ? name + "()" : name;
	}
}
