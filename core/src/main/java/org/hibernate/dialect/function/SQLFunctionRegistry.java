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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;

public class SQLFunctionRegistry {

	private final Dialect dialect;
	private final Map userFunctions;
	
	public SQLFunctionRegistry(Dialect dialect, Map userFunctions) {
		this.dialect = dialect;
		this.userFunctions = new HashMap();
		this.userFunctions.putAll( userFunctions );
	}
	
	public SQLFunction findSQLFunction(String functionName) {
		String name = functionName.toLowerCase();
		SQLFunction userFunction = (SQLFunction) userFunctions.get( name );
		
		return userFunction!=null?userFunction:(SQLFunction) dialect.getFunctions().get(name); // TODO: lowercasing done here. Was done "at random" before; maybe not needed at all ?
	}

	public boolean hasFunction(String functionName) {
		String name = functionName.toLowerCase();
		boolean hasUserFunction = userFunctions.containsKey ( name );
		
		return hasUserFunction || dialect.getFunctions().containsKey ( name ); // TODO: toLowerCase was not done before. Only used in Template.
	}

}
