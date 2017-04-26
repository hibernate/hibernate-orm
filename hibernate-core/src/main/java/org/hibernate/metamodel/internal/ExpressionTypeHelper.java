/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Helper for making the determination of an expression's "type" as sovered by the rules
 * defined in section 6.5.7.1 (Result Types of Expressions) of the JPA 2.1 spec
 *
 * @author Steve Ebersole
 */
public class ExpressionTypeHelper {
	private ExpressionTypeHelper() {
	}

	public static BasicValuedExpressableType resolveArithmeticType(
			BasicValuedExpressableType firstType,
			BasicValuedExpressableType secondType,
			boolean isDivision,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.resolveArithmeticType( firstType, secondType, isDivision );
	}
}
