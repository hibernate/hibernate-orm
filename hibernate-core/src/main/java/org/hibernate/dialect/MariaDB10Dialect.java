/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.type.spi.StandardSpiBasicTypes;

public class MariaDB10Dialect extends MariaDB53Dialect {

	public MariaDB10Dialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );
		registry.registerNamed( "regexp_replace", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "regexp_instr", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "regexp_substr", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "weight_string", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "to_base64", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "from_base64", StandardSpiBasicTypes.STRING );
	}

		@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}
}
