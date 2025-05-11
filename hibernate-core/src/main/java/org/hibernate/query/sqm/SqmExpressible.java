/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.query.spi.BindableTypeImplementor;
import org.hibernate.query.spi.BindingContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Anything in the application domain model that can be used in an
 * SQM query as an expression
 *
 * @see SqmExpression#getNodeType
 *
 * @author Steve Ebersole
 */
public interface SqmExpressible<J> extends BindableTypeImplementor<J> {
	/**
	 * The Java type descriptor for this expressible
	 */
	JavaType<J> getExpressibleJavaType();

	default JavaType<?> getRelationalJavaType() {
		return getExpressibleJavaType();
	}

	@Override
	default SqmExpressible<J> resolveExpressible(BindingContext bindingContext) {
		return this;
	}

	/**
	 * The name of the type.
	 *
	 * @apiNote This is the Hibernate notion of the type name. For most
	 *          types this is just the Java type ({@link Class}) name.
	 *          However, using the string allows for dynamic models.
	 */
	default String getTypeName() {
		// default impl to handle the general case returning the Java type name
		final JavaType<J> expressibleJavaType = getExpressibleJavaType();
		return expressibleJavaType == null ? "unknown" : expressibleJavaType.getTypeName();
	}

	SqmDomainType<J> getSqmType();
}
