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
import java.util.HashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;

public class SQLFunctionRegistry {
	private final Dialect dialect;
	private final Map<String, SQLFunction> userFunctions;
	
	public SQLFunctionRegistry(Dialect dialect, Map<String, SQLFunction> userFunctions) {
		this.dialect = dialect;
		this.userFunctions = new HashMap<String, SQLFunction>();
		this.userFunctions.putAll( userFunctions );
	}
	
	public SQLFunction findSQLFunction(String functionName) {
		String name = functionName.toLowerCase();
		SQLFunction userFunction = userFunctions.get( name );
		return userFunction != null
				? userFunction
				: (SQLFunction) dialect.getFunctions().get( name );
	}

	public boolean hasFunction(String functionName) {
		String name = functionName.toLowerCase();
		return userFunctions.containsKey( name ) || dialect.getFunctions().containsKey( name );
	}

}
