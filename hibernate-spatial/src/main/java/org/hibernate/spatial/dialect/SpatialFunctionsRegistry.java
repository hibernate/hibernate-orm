/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;

/**
 * Registers all available spatial functions for a <code>Dialect</code>
 *
 * Created by Karel Maesen, Geovise BVBA on 29/10/16.
 */
public abstract class SpatialFunctionsRegistry implements Iterable<Map.Entry<String, StandardSQLFunction>> {
	protected final Map<String, StandardSQLFunction> functionMap = new HashMap<String, StandardSQLFunction>();

	public void put(String name, StandardSQLFunction function ) {
		this.functionMap.put( name, function );
	}

	@Override
	public Iterator<Map.Entry<String, StandardSQLFunction>> iterator() {
		return functionMap.entrySet().iterator();
	}

	public SQLFunction get(String functionName) {
		return functionMap.get( functionName );
	}
}
