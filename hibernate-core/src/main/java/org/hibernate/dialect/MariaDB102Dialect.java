/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import java.sql.Types;

public class MariaDB102Dialect extends MariaDB10Dialect {

	public MariaDB102Dialect() {
		super();

		this.registerColumnType( Types.JAVA_OBJECT, "json" );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerNamed( "json_valid", StandardSpiBasicTypes.NUMERIC_BOOLEAN );
	}

		@Override
	public boolean supportsColumnCheck() {
		return true;
	}
}
