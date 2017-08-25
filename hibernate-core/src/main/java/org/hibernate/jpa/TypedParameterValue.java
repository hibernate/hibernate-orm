/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.type.Type;

/**
 * Can be used to bind query parameter values.  Allows to provide additional details about the
 * parameter value/binding.
 *
 * @author Steve Ebersole
 *
 * @deprecated (since 6.0) use {@link org.hibernate.query.TypedParameterValue} instead.
 */
@Deprecated
public class TypedParameterValue extends org.hibernate.query.TypedParameterValue {
	public TypedParameterValue(Type type, Object value) {
		super( (AllowableParameterType) type, value );
	}
}
