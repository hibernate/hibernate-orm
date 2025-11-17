/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.IncomparableComparator;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Uses object identity for {@code equals}/{@code hashCode} as we ensure that internally.
 *
 * @author Christian Beikov
 */
public class EntityJavaType<T> extends AbstractClassJavaType<T> {

	public EntityJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan , IncomparableComparator.INSTANCE );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		throw new JdbcTypeRecommendationException(
				"Could not determine recommended JdbcType for '" + getTypeName() + "'"
		);
	}

	@Override
	public int extractHashCode(T value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(T one, T another) {
		return one == another;
	}

	@Override
	public boolean isInstance(Object value) {
		final var lazyInitializer = extractLazyInitializer( value );
		final var javaTypeClass = getJavaTypeClass();
		if ( lazyInitializer != null ) {
			return javaTypeClass.isAssignableFrom( lazyInitializer.getPersistentClass() )
				|| javaTypeClass.isAssignableFrom( lazyInitializer.getImplementationClass() );
		}
		else {
			return javaTypeClass.isAssignableFrom( value.getClass() );
		}
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}

	@Override
	public T fromString(CharSequence string) {
		throw new UnsupportedOperationException(
				"Conversion from String strategy not known for this Java type: " + getTypeName()
		);
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
		return "EntityJavaType(" + getTypeName() + ")";
	}
}
