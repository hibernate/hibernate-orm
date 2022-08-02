/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.SqmExpressible;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.metamodel.ManagedType;

/**
 * Types that can be used to handle binding {@link Query} parameters
 *
 * @see org.hibernate.type.BasicTypeReference
 * @see org.hibernate.type.StandardBasicTypes
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BindableType<J> {
	/**
	 * The expected Java type
	 */
	Class<J> getBindableJavaType();

	default boolean isInstance(J value) {
		return getBindableJavaType().isInstance( value );
	}

	static <T> BindableType<? extends T> parameterType(Class<T> type) {
		throw new NotYetImplementedFor6Exception( "BindableType#parameterType" );
	}

	static <T> BindableType<? extends T> parameterType(Class<?> javaType, AttributeConverter<T,?> converter) {
		throw new NotYetImplementedFor6Exception( "BindableType#parameterType" );
	}

	static <T> BindableType<? extends T> parameterType(Class<?> javaType, Class<? extends AttributeConverter<T,?>> converter) {
		throw new NotYetImplementedFor6Exception( "BindableType#parameterType" );
	}

	static <T> BindableType<? extends T> parameterType(ManagedType<T> managedType) {
		throw new NotYetImplementedFor6Exception( "BindableType#parameterType" );
	}

	static <T> BindableType<? extends T> parameterType(jakarta.persistence.metamodel.Bindable<T> jpaBindable) {
		throw new NotYetImplementedFor6Exception( "BindableType#parameterType" );
	}

	static <T> BindableType<? extends T> parameterType(org.hibernate.metamodel.mapping.Bindable bindable) {
		throw new NotYetImplementedFor6Exception( "BindableType#parameterType" );
	}

	/**
	 * Resolve this parameter type to the corresponding SqmExpressible
	 *
	 * @todo (6.0) - use SessionFactory (API) here instead - we'll just cast "below"
	 */
	SqmExpressible<J> resolveExpressible(SessionFactoryImplementor sessionFactory);
}
