/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Allows the implementations of a custom logic used to retrieve the value
 * for a {@link org.hibernate.annotations.Filter}'s parameter.
 * <p>
 * The parameter's value can be set using A filter may be defined either using
 * {@link org.hibernate.Filter setParameter} or by providing a resolver,
 * that will be executed to retrieve the value.
 * <p>
 * If there is a CDI context, the resolver can be defined as a Bean, and
 * the bean itself will be retrieved from the context. Otherwise, the resolver
 * will be created as a new instance of the class.
 *
 * @see org.hibernate.annotations.ParamDef
 * @see org.hibernate.annotations.FilterDef
 *
 * @author Gregorio Palamà
 */
public interface FilterParamResolver {
	/**
	 * The custom logic that will be used to retrieve a
	 * {@link org.hibernate.annotations.ParamDef}'s value.
	 *
	 * @return The param's value.
	 */
	Object resolve();
}
