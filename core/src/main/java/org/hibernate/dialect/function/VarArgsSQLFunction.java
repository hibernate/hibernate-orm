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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Support for slightly more general templating than {@link StandardSQLFunction},
 * with an unlimited number of arguments.
 *
 * @author Gavin King
 */
public class VarArgsSQLFunction implements SQLFunction {
	private final String begin;
	private final String sep;
	private final String end;
	private final Type type;

	/**
	 * Constructs a VarArgsSQLFunction instance with a 'static' return type.  An example of a 'static'
	 * return type would be something like an <tt>UPPER</tt> function which is always returning
	 * a SQL VARCHAR and thus a string type.
	 *
	 * @param type The return type.
	 * @param begin The beginning of the function templating.
	 * @param sep The separator for each individual function argument.
	 * @param end The end of the function templating.
	 */
	public VarArgsSQLFunction(Type type, String begin, String sep, String end) {
		this.type = type;
		this.begin = begin;
		this.sep = sep;
		this.end = end;
	}

	/**
	 * Constructs a VarArgsSQLFunction instance with a 'dynamic' return type.  For a dynamic return type,
	 * the type of the arguments are used to resolve the type.  An example of a function with a
	 * 'dynamic' return would be <tt>MAX</tt> or <tt>MIN</tt> which return a double or an integer etc
	 * based on the types of the arguments.
	 *
	 * @param begin The beginning of the function templating.
	 * @param sep The separator for each individual function argument.
	 * @param end The end of the function templating.
	 *
	 * @see #getReturnType Specifically, the 'columnType' argument is the 'dynamic' type.
	 */
	public VarArgsSQLFunction(String begin, String sep, String end) {
		this( null, begin, sep, end );
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return type == null ? columnType : type;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Always returns true here.
	 */
	public boolean hasArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Always returns true here.
	 */
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		StringBuffer buf = new StringBuffer().append( begin );
		for ( int i = 0; i < args.size(); i++ ) {
			buf.append( transformArgument( ( String ) args.get( i ) ) );
			if ( i < args.size() - 1 ) {
				buf.append( sep );
			}
		}
		return buf.append( end ).toString();
	}

	/**
	 * Called from {@link #render} to allow applying a change or transformation to each individual
	 * argument.
	 *
	 * @param argument The argument being processed.
	 * @return The transformed argument; may be the same, though should never be null.
	 */
	protected String transformArgument(String argument) {
		return argument;
	}
}
