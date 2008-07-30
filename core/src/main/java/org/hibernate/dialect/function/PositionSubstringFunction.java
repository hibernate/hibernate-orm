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

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Emulation of <tt>locate()</tt> on PostgreSQL
 * @author Gavin King
 */
public class PositionSubstringFunction implements SQLFunction {

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return Hibernate.INTEGER;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		boolean threeArgs = args.size() > 2;
		Object pattern = args.get(0);
		Object string = args.get(1);
		Object start = threeArgs ? args.get(2) : null;

		StringBuffer buf = new StringBuffer();
		if (threeArgs) buf.append('(');
		buf.append("position(").append( pattern ).append(" in ");
		if (threeArgs) buf.append( "substring(");
		buf.append( string );
		if (threeArgs) buf.append( ", " ).append( start ).append(')');
		buf.append(')');
		if (threeArgs) buf.append('+').append( start ).append("-1)");
		return buf.toString();
	}


}
