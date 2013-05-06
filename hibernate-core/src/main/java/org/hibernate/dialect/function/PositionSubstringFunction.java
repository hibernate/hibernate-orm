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
