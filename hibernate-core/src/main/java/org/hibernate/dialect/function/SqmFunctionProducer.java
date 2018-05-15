/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Helper for Dialect-specific function definitions that allow composition
 * of several function calls.
 *
 * @author Steve Ebersole
 */
public interface SqmFunctionProducer {
	SqmExpression produce(SqmFunctionRegistry registry, AllowableFunctionReturnType type, List<SqmExpression> arguments);
}
