/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBindings {
	boolean isBound(QueryParameter parameter);

	<T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter);
	<T> QueryParameterBinding<T> getBinding(String name);
	<T> QueryParameterBinding<T> getBinding(int position);

	void verifyParametersBound(boolean callable);
	String expandListValuedParameters(String queryString, SharedSessionContractImplementor producer);

	<T> QueryParameterListBinding<T> getQueryParameterListBinding(QueryParameter<T> parameter);
	<T> QueryParameterListBinding<T> getQueryParameterListBinding(String name);
	<T> QueryParameterListBinding<T> getQueryParameterListBinding(int position);

	Type[] collectPositionalBindTypes();
	Object[] collectPositionalBindValues();
	Map<String,TypedValue> collectNamedParameterBindings();
}
