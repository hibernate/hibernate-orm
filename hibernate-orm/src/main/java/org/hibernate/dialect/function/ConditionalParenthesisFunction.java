/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Essentially the same as {@link org.hibernate.dialect.function.StandardSQLFunction},
 * except that here the parentheses are not included when no arguments are given.
 *
 * @author Jonathan Levinson
 */
public class ConditionalParenthesisFunction extends StandardSQLFunction {
	/**
	 * Constructs a ConditionalParenthesisFunction with the given name
	 *
	 * @param name The function name
	 */
	public ConditionalParenthesisFunction(String name) {
		super( name );
	}

	/**
	 * Constructs a ConditionalParenthesisFunction with the given name
	 *
	 * @param name The function name
	 * @param type The function return type
	 */
	public ConditionalParenthesisFunction(String name, Type type) {
		super( name, type );
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	@Override
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
		final boolean hasArgs = !arguments.isEmpty();
		final StringBuilder buf = new StringBuilder( getName() );
		if ( hasArgs ) {
			buf.append( "(" );
			for ( int i = 0; i < arguments.size(); i++ ) {
				buf.append( arguments.get( i ) );
				if ( i < arguments.size() - 1 ) {
					buf.append( ", " );
				}
			}
			buf.append( ")" );
		}
		return buf.toString();
	}
}
