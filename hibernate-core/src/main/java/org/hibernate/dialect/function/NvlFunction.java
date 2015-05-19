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
 * Emulation of <tt>coalesce()</tt> on Oracle, using multiple <tt>nvl()</tt> calls
 *
 * @author Gavin King
 */
public class NvlFunction implements SQLFunction {
	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	@Override
	public Type getReturnType(Type argumentType, Mapping mapping) throws QueryException {
		return argumentType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String render(Type argumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		final int lastIndex = args.size()-1;
		final Object last = args.remove( lastIndex );
		if ( lastIndex==0 ) {
			return last.toString();
		}
		final Object secondLast = args.get( lastIndex-1 );
		final String nvl = "nvl(" + secondLast + ", " + last + ")";
		args.set( lastIndex-1, nvl );
		return render( argumentType, args, factory );
	}
}
