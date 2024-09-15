/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.SqmExpressible;

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

	/**
	 * Resolve this parameter type to the corresponding {@link SqmExpressible}
	 */
	SqmExpressible<J> resolveExpressible(BindingContext bindingContext);
}
