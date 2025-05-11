/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A context within which a {@link BindableType} can be resolved
 * to an instance of {@link org.hibernate.query.sqm.SqmExpressible}.
 *
 * @author Gavin King
 *
 * @since 7.0
 *
 * @see BindableTypeImplementor#resolveExpressible(BindingContext)
 * @see org.hibernate.query.sqm.SqmExpressible#resolveExpressible(BindingContext)
 * @see org.hibernate.query.sqm.produce.function.ArgumentsValidator#validate(java.util.List, String, BindingContext)
 * @see org.hibernate.query.sqm.internal.TypecheckUtil
 */
@Incubating
public interface BindingContext {
	JpaMetamodel getJpaMetamodel();

	MappingMetamodel getMappingMetamodel();

	TypeConfiguration getTypeConfiguration();

	/**
	 * Resolve this parameter type to the corresponding {@link SqmExpressible}.
	 *
	 * @param bindableType the {@link BindableType} representing the type of a parameter,
	 *                     which may be null if the type is not known
	 * @return the corresponding {@link SqmExpressible}, or null if the argument was null
	 * @param <J> the type of the parameter
	 *
	 * @since 7.0
	 */
	default <J> SqmExpressible<J> resolveExpressible(BindableType<J> bindableType) {
		if ( bindableType == null ) {
			return null;
		}
		else if ( bindableType instanceof BindableTypeImplementor<J> implementor) {
			return implementor.resolveExpressible( this );
		}
		else {
			throw new IllegalArgumentException( "BindableType does not implement BindableTypeImplementor" );
		}
	}
}
