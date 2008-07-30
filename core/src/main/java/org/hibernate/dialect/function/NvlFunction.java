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
 * Emulation of <tt>coalesce()</tt> on Oracle, using multiple
 * <tt>nvl()</tt> calls
 * @author Gavin King
 */
public class NvlFunction implements SQLFunction {

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return columnType;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		int lastIndex = args.size()-1;
		Object last = args.remove(lastIndex);
		if ( lastIndex==0 ) return last.toString();
		Object secondLast = args.get(lastIndex-1);
		String nvl = "nvl(" + secondLast + ", " + last + ")";
		args.set(lastIndex-1, nvl);
		return render(args, factory);
	}

	

}
