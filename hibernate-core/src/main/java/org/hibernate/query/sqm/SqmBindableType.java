/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.type.BindableType;
import org.hibernate.type.BindingContext;

/**
 * An SQM node which may be used to disambiguate the type of an argument to a query parameter.
 *
 * @author Gavin King
 *
 * @since 7.0
 */
public interface SqmBindableType<J> extends SqmExpressible<J>, BindableType<J> {
	@Override
	default SqmBindableType<J> resolveExpressible(BindingContext bindingContext) {
		return this;
	}
}
