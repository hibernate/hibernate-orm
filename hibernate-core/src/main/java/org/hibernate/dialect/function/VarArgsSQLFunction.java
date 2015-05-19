/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Support for slightly more general templating than {@link StandardSQLFunction}, with an unlimited number of arguments.
 *
 * @author Gavin King
 */
public class VarArgsSQLFunction implements SQLFunction {
	private final String begin;
	private final String sep;
	private final String end;
	private final Type registeredType;

	/**
	 * Constructs a VarArgsSQLFunction instance with a 'static' return type.  An example of a 'static'
	 * return type would be something like an <tt>UPPER</tt> function which is always returning
	 * a SQL VARCHAR and thus a string type.
	 *
	 * @param registeredType The return type.
	 * @param begin The beginning of the function templating.
	 * @param sep The separator for each individual function argument.
	 * @param end The end of the function templating.
	 */
	public VarArgsSQLFunction(Type registeredType, String begin, String sep, String end) {
		this.registeredType = registeredType;
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
	 * @see #getReturnType Specifically, the 'firstArgumentType' argument is the 'dynamic' type.
	 */
	public VarArgsSQLFunction(String begin, String sep, String end) {
		this( null, begin, sep, end );
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	@Override
	public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
		return registeredType == null ? firstArgumentType : registeredType;
	}

	@Override
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
		final StringBuilder buf = new StringBuilder().append( begin );
		for ( int i = 0; i < arguments.size(); i++ ) {
			buf.append( transformArgument( (String) arguments.get( i ) ) );
			if ( i < arguments.size() - 1 ) {
				buf.append( sep );
			}
		}
		return buf.append( end ).toString();
	}

	/**
	 * Called from {@link #render} to allow applying a change or transformation
	 * to each individual argument.
	 *
	 * @param argument The argument being processed.
	 * @return The transformed argument; may be the same, though should never be null.
	 */
	protected String transformArgument(String argument) {
		return argument;
	}
}
