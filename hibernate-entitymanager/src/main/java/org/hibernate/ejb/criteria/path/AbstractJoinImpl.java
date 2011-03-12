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

import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.CriteriaSubqueryImpl;
import org.hibernate.ejb.criteria.FromImplementor;
import org.hibernate.ejb.criteria.JoinImplementor;
import org.hibernate.ejb.criteria.PathSource;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJoinImpl<Z, X>
		extends AbstractFromImpl<Z, X>
		implements JoinImplementor<Z,X>, Serializable {

	private final Attribute<? super Z, ?> joinAttribute;
	private final JoinType joinType;

	public AbstractJoinImpl(
			CriteriaBuilderImpl criteriaBuilder,
			PathSource<Z> pathSource,
			Attribute<? super Z, X> joinAttribute,
			JoinType joinType) {
		this( criteriaBuilder, joinAttribute.getJavaType(), pathSource, joinAttribute, joinType );
	}

	public AbstractJoinImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathSource<Z> pathSource,
			Attribute<? super Z, ?> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource );
		this.joinAttribute = joinAttribute;
		this.joinType = joinType;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribute<? super Z, ?> getAttribute() {
		return joinAttribute;
	}

	/**
	 * {@inheritDoc}
	 */
	public JoinType getJoinType() {
		return joinType;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public From<?, Z> getParent() {
		// this cast should be ok by virtue of our constructors...
		return (From<?, Z>) getPathSource();
	}

	public String renderTableExpression(CriteriaQueryCompiler.RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		( (FromImplementor) getParent() ).prepareAlias( renderingContext );
		return getParent().getAlias() + '.' + getAttribute().getName() + " as " + getAlias();
	}


	public JoinImplementor<Z, X> correlateTo(CriteriaSubqueryImpl subquery) {
		return (JoinImplementor<Z, X>) super.correlateTo( subquery );
	}
}
