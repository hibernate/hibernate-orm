/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.spi.StandardSpiBasicTypes;

public class MariaDB10Dialect extends MariaDB53Dialect {

	public MariaDB10Dialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );
		queryEngine.getSqmFunctionRegistry().registerNamed( "regexp_replace", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "regexp_instr", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "regexp_substr", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "weight_string", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "to_base64", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "from_base64", StandardSpiBasicTypes.STRING );
	}

		@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}
}
