/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.dialect.function;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * When there is need to use round function with other function (i.e.sum()),
 * its return type becomes Double by default if it is registered as a StandardSQLFunction.
 * This function provides the feasibility to return BigDecimal as return type if argument is
 * BigDecimal
 *
 * @author Mayur Bhindi
 */
public class RoundFunction extends StandardSQLFunction {

	/**
	 * Constructs RoundFunction
	 *
	 * @param name
	 */
	public RoundFunction(String name) {
		super( name );
	}

	@Override
	public Type getReturnType(Type firstArgumentType, Mapping mapping) {
		return firstArgumentType == StandardBasicTypes.BIG_DECIMAL ? firstArgumentType : StandardBasicTypes.DOUBLE;
	}

}
