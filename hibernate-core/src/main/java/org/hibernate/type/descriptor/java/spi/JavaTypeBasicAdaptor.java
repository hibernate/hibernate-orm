/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * {@link AbstractClassJavaType} for cases where we do not know a proper
 * {@link org.hibernate.type.descriptor.java.JavaType} for a given Java type.
 *
 * @author Steve Ebersole
 */
public class JavaTypeBasicAdaptor<T> extends AbstractClassJavaType<T> {
	public JavaTypeBasicAdaptor(Class<T> type) {
		super( type );
	}

	public JavaTypeBasicAdaptor(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		throw new JdbcTypeRecommendationException(
				"Could not determine recommended JdbcType for '" + getTypeName() + "'"
		);
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Unwrap strategy not known for this Java type: " + getTypeName()
		);
	}

	@Override
	public <X> T wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Wrap strategy not known for this Java type: " + getTypeName()
		);
	}

	@Override
	public String toString() {
		return "JavaTypeBasicAdaptor(" + getTypeName() + ")";
	}
}
