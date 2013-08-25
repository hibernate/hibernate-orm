/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
