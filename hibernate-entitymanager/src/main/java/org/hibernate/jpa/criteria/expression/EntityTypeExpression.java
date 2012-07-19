/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EntityTypeExpression<T> extends ExpressionImpl<T> implements Serializable {
	public EntityTypeExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothign to do
	}

	public String render(RenderingContext renderingContext) {
		// todo : is it valid for this to get rendered into the query itself?
		throw new IllegalArgumentException( "Unexpected call on EntityTypeExpression#render" );
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
