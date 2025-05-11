/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.query.spi.BindableTypeImplementor;
import org.hibernate.query.spi.BindingContext;

/**
 * @param <J>
 * @author Gavin King
 * @since 7.0
 */
public interface SqmBindable<J> extends SqmExpressible<J>, BindableTypeImplementor<J> {
	@Override
	default SqmBindable<J> resolveExpressible(BindingContext bindingContext) {
		return this;
	}
}
