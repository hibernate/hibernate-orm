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
package org.hibernate.ejb.criteria.expression;

import javax.persistence.metamodel.ListAttribute;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.PathImpl;

/**
 * An expression for referring to the index of a list.
 *
 * @author Steve Ebersole
 */
public class ListIndexExpression extends ExpressionImpl<Integer> {
	private final PathImpl origin;
	private final ListAttribute<?,?> listAttribute;

	public ListIndexExpression(CriteriaBuilderImpl criteriaBuilder, PathImpl origin, ListAttribute<?,?> listAttribute) {
		super( criteriaBuilder, Integer.class );
		this.origin = origin;
		this.listAttribute = listAttribute;
	}

	public ListAttribute<?,?> getListAttribute() {
		return listAttribute;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothign to do
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return "index("
				+ origin.getPathIdentifier() + '.' + getListAttribute().getName()
				+ ")";
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
