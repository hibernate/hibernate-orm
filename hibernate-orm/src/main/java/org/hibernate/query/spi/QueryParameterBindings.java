/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBindings {
	boolean isBound(QueryParameter parameter);

	<T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter);
	<T> QueryParameterBinding<T> getBinding(String name);
	<T> QueryParameterBinding<T> getBinding(int position);
}
