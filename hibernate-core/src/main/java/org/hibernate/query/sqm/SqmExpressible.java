/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Anything in the application domain model that can be used in an
 * SQM query as an expression.
 *
 * @see SqmExpression#getNodeType
 *
 * @author Steve Ebersole
 */
public interface SqmExpressible<J> {
	/**
	 * The Java type descriptor for this expressible
	 */
	JavaType<J> getExpressibleJavaType();

	/**
	 * Usually the same as {@link #getExpressibleJavaType()}. But for types with
	 * {@linkplain org.hibernate.type.descriptor.converter.spi.BasicValueConverter
	 * value conversion}, the Java type of the converted value.
	 */
	default JavaType<?> getRelationalJavaType() {
		return getExpressibleJavaType();
	}

	/**
	 * The name of the type. Usually, but not always, the name of a Java class.
	 *
	 * @see org.hibernate.metamodel.model.domain.DomainType#getTypeName()
	 * @see JavaType#getTypeName()
	 */
	default String getTypeName() {
		// default impl to handle the general case returning the Java type name
		final JavaType<J> expressibleJavaType = getExpressibleJavaType();
		return expressibleJavaType == null ? "unknown" : expressibleJavaType.getTypeName();
	}

	@Nullable SqmDomainType<J> getSqmType();
}
