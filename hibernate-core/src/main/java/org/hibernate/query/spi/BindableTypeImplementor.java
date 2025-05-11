/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.SqmBindable;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * SPI-level interface which must be implemented by every implementation of
 * {@link BindableType}.
 *
 * @param <J> the type of the parameter
 *
 * @apiNote This was introduced to eliminate the leakage of {@link SqmExpressible}
 *          and {@link org.hibernate.type.spi.TypeConfiguration} into the API
 *          package {@link org.hibernate.query}.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
@Incubating
public interface BindableTypeImplementor<J> extends BindableType<J> {
	/**
	 * Resolve this parameter type to the corresponding {@link SqmExpressible}.
	 */
	SqmBindable<J> resolveExpressible(BindingContext bindingContext);
}
