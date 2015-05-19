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
 * Emulation of <tt>locate()</tt> on Sybase
 *
 * @author Nathan Moon
 */
public class CharIndexFunction implements SQLFunction {
	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	@Override
	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return StandardBasicTypes.INTEGER;
	}

	@Override
	public String render(Type columnType, List args, SessionFactoryImplementor factory) throws QueryException {
		final boolean threeArgs = args.size() > 2;
		final Object pattern = args.get( 0 );
		final Object string = args.get( 1 );
		final Object start = threeArgs ? args.get( 2 ) : null;

		final StringBuilder buf = new StringBuilder();
		buf.append( "charindex(" ).append( pattern ).append( ", " );
		if (threeArgs) {
			buf.append( "right(" );
		}
		buf.append( string );
		if (threeArgs) {
			buf.append( ", char_length(" ).append( string ).append( ")-(" ).append( start ).append( "-1))" );
		}
		buf.append( ')' );
		return buf.toString();
	}

}
