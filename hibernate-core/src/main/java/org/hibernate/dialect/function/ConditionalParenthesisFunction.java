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

import org.hibernate.engine.spi.SessionFactoryImplementor;
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
		StringBuilder buf = new StringBuilder();
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
