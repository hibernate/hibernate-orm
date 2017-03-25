/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.dialect.function.SQLFunction;

/**
 * Registers all available spatial functions for a <code>Dialect</code>
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 29/10/16.
 */
public abstract class SpatialFunctionsRegistry implements Iterable<Map.Entry<String, SQLFunction>>, Serializable {
	protected final Map<String, SQLFunction> functionMap = new HashMap<String, SQLFunction>();

	public void put(String name, SQLFunction function) {
		this.functionMap.put( name, function );
	}

	@Override
	public Iterator<Map.Entry<String, SQLFunction>> iterator() {
		return functionMap.entrySet().iterator();
	}

	public SQLFunction get(String functionName) {
		return functionMap.get( functionName );
	}
}
