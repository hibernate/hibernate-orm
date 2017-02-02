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
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Emulation of <tt>locate()</tt> on PostgreSQL
 *
 * @author Gavin King
 */
public class PositionSubstringFunction implements SQLFunction {
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
		return StandardBasicTypes.INTEGER;
	}

	@Override
	public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		final boolean threeArgs = args.size() > 2;
		final Object pattern = args.get( 0 );
		final Object string = args.get( 1 );
		final Object start = threeArgs ? args.get( 2 ) : null;

		final StringBuilder buf = new StringBuilder();
		if (threeArgs) {
			buf.append( '(' );
		}
		buf.append( "position(" ).append( pattern ).append( " in " );
		if (threeArgs) {
			buf.append( "substring(");
		}
		buf.append( string );
		if (threeArgs) {
			buf.append( ", " ).append( start ).append( ')' );
		}
		buf.append( ')' );
		if (threeArgs) {
			buf.append( '+' ).append( start ).append( "-1)" );
		}
		return buf.toString();
	}


}
