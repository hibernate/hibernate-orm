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

import java.io.Serializable;
import java.util.Collection;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.path.PluralAttributePath;

/**
 * Represents a "size of" expression in regards to a persistent collection; the implication is
 * that of a subquery.
 *
 * @author Steve Ebersole
 */
public class SizeOfCollectionExpression<C extends Collection>
		extends ExpressionImpl<Integer>
		implements Serializable {
	private final PluralAttributePath<C> collectionPath;

	public SizeOfCollectionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			PluralAttributePath<C> collectionPath) {
		super( criteriaBuilder, Integer.class);
		this.collectionPath = collectionPath;
	}

	public PluralAttributePath<C> getCollectionPath() {
		return collectionPath;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return "size(" + getCollectionPath().render( renderingContext ) + ")";
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
