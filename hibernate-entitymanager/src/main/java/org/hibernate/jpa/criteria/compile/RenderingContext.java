/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.compile;

import javax.persistence.criteria.ParameterExpression;

/**
 * Used to provide a context and services to the rendering.
 *
 * @author Steve Ebersole
 */
public interface RenderingContext {
	/**
	 * Generate a correlation name.
	 *
	 * @return The generated correlation name
	 */
	public String generateAlias();

	/**
	 * Register parameters explicitly encountered in the criteria query.
	 *
	 * @param criteriaQueryParameter The parameter expression
	 *
	 * @return The JPA-QL parameter name
	 */
	public ExplicitParameterInfo registerExplicitParameter(ParameterExpression<?> criteriaQueryParameter);

	/**
	 * Register a parameter that was not part of the criteria query (at least not as a parameter).
	 *
	 * @param literal The literal value
	 * @param javaType The java type as whcih to handle the literal value.
	 *
	 * @return The JPA-QL parameter name
	 */
	public String registerLiteralParameterBinding(Object literal, Class javaType);

	/**
	 * Given a java type, determine the proper cast type name.
	 *
	 * @param javaType The java type.
	 *
	 * @return The cast type name.
	 */
	public String getCastType(Class javaType);
}
