//$Id: ConditionalParenthesisFunction.java,v 1.4 2005/04/26 18:08:01 oneovthafew Exp $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Essentially the same as {@link org.hibernate.dialect.function.StandardSQLFunction},
 * except that here the parentheses are not included when no arguments are given.
 *
 * @author Jonathan Levinson
 */
public class ConditionalParenthesisFunction extends StandardSQLFunction {

	public ConditionalParenthesisFunction(String name) {
		super( name );
	}

	public ConditionalParenthesisFunction(String name, Type type) {
		super( name, type );
	}

	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	public String render(List args, SessionFactoryImplementor factory) {
		final boolean hasArgs = !args.isEmpty();
		StringBuffer buf = new StringBuffer();
		buf.append( getName() );
		if ( hasArgs ) {
			buf.append( "(" );
			for ( int i = 0; i < args.size(); i++ ) {
				buf.append( args.get( i ) );
				if ( i < args.size() - 1 ) {
					buf.append( ", " );
				}
			}
			buf.append( ")" );
		}
		return buf.toString();
	}
}
