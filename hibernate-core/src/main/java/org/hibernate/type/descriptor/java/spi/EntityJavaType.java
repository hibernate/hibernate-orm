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

import static java.lang.System.identityHashCode;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Uses object identity for {@code equals}/{@code hashCode} as we ensure that internally.
 *
 * @author Christian Beikov
 */
public class EntityJavaType<T> extends AbstractClassJavaType<T> {

	public EntityJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan, IncomparableComparator.INSTANCE );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getTypeConfiguration().getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(getJavaTypeClass())
				.getIdentifierDescriptor()
				.getJavaType()
				.getRecommendedJdbcType( context );
	}

	@Override
	public int extractHashCode(T value) {
		return identityHashCode( value );
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

	@Override @SuppressWarnings("unchecked") // safe, we do check
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		final var id =
				options.getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( getJavaTypeClass() )
						.getIdentifier( value );
		if ( !type.isInstance( id ) ) {
			throw new IllegalArgumentException( "Id not an instance of type " + type.getName() );
		}
		return (X) value;
	}

	@Override @SuppressWarnings("unchecked") // safe, we do check
	public <X> T wrap(X value, WrapperOptions options) {
		final var persister =
				options.getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( getJavaTypeClass() );
		final var idType = persister.getIdentifierType().getReturnedClass();
		if ( !idType.isInstance( value ) ) {
			throw new IllegalArgumentException( "Not an instance of id type " + idType.getName() );
		}
		final var entity =
				options.getSession()
						.internalLoad( persister.getEntityName(), value, false, true );
		return (T) entity;
	}

	@Override
	public String toString() {
		return "EntityJavaType(" + getTypeName() + ")";
	}
}
