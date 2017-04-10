/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.util.LinkedHashMap;

/**
 * @author Andrea Boriero
 */
public class OrderedMapType extends MapType {

	public OrderedMapType(String roleName) {
		super( roleName );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize > 0
				? new LinkedHashMap( anticipatedSize )
				: new LinkedHashMap();
	}
}
