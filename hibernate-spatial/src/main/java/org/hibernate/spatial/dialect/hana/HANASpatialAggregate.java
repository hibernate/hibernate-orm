/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.Type;

public class HANASpatialAggregate extends StandardSQLFunction {

	public HANASpatialAggregate(String name) {
		super( name );
	}

	public HANASpatialAggregate(String name, Type registeredType) {
		super( name, registeredType );
	}
}
