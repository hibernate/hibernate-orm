/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.criteria.path;

import java.io.Serializable;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.CriteriaSubqueryImpl;
import org.hibernate.ejb.criteria.FromImplementor;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class RootImpl<X> extends AbstractFromImpl<X,X> implements Root<X>, Serializable {
	private final EntityType<X> entityType;

	public RootImpl(
			CriteriaBuilderImpl criteriaBuilder,
			EntityType<X> entityType) {
		super( criteriaBuilder, entityType.getJavaType() );
		this.entityType = entityType;
	}

	public EntityType<X> getEntityType() {
		return entityType;
	}

	public EntityType<X> getModel() {
		return getEntityType();
	}

	@Override
	protected FromImplementor<X, X> createCorrelationDelegate() {
		return new RootImpl<X>( criteriaBuilder(), getEntityType() );
	}

	@Override
	public RootImpl<X> correlateTo(CriteriaSubqueryImpl subquery) {
		return (RootImpl<X>) super.correlateTo( subquery );
	}

	@Override
	protected boolean canBeJoinSource() {
		return true;
	}

	public String renderTableExpression(CriteriaQueryCompiler.RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		return getModel().getName() + " as " + getAlias();
	}

	@Override
	public String getPathIdentifier() {
		return getAlias();
	}

	@Override
	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		return getAlias();
	}

	@Override
	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
