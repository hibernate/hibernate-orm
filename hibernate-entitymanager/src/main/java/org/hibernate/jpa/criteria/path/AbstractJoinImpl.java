/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, 2012 Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.criteria.path;

import java.io.Serializable;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.CriteriaSubqueryImpl;
import org.hibernate.jpa.criteria.FromImplementor;
import org.hibernate.jpa.criteria.JoinImplementor;
import org.hibernate.jpa.criteria.PathSource;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.predicate.AbstractPredicateImpl;

/**
 * Convenience base class for various {@link javax.persistence.criteria.Join} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJoinImpl<Z, X>
		extends AbstractFromImpl<Z, X>
		implements JoinImplementor<Z,X>, Serializable {

	private final Attribute<? super Z, ?> joinAttribute;
	private final JoinType joinType;

	private Predicate suppliedJoinCondition;

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

	@Override
	public Attribute<? super Z, ?> getAttribute() {
		return joinAttribute;
	}

	@Override
	public JoinType getJoinType() {
		return joinType;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public From<?, Z> getParent() {
		// this cast should be ok by virtue of our constructors...
		return (From<?, Z>) getPathSource();
	}

	@Override
	public String renderTableExpression(RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		( (FromImplementor) getParent() ).prepareAlias( renderingContext );
		StringBuilder tableExpression = new StringBuilder();
		tableExpression.append( getParent().getAlias() )
				.append( '.' )
				.append( getAttribute().getName() )
				.append( " as " )
				.append( getAlias() );
		if ( suppliedJoinCondition != null ) {
			tableExpression.append( " with " )
					.append( ( (AbstractPredicateImpl) suppliedJoinCondition ).render( renderingContext ) );
		}
		return tableExpression.toString();
	}

	@Override
	public JoinImplementor<Z, X> correlateTo(CriteriaSubqueryImpl subquery) {
		return (JoinImplementor<Z, X>) super.correlateTo( subquery );
	}

	@Override
	public JoinImplementor<Z, X> on(Predicate... restrictions) {
		// no matter what, a call to this method replaces any previously set values...
		this.suppliedJoinCondition = null;

		if ( restrictions != null && restrictions.length > 0 ) {
			this.suppliedJoinCondition = criteriaBuilder().and( restrictions );
		}

		return this;
	}

	@Override
	public JoinImplementor<Z, X> on(Expression<Boolean> restriction) {
		this.suppliedJoinCondition = criteriaBuilder().wrap( restriction );
		return this;
	}

	@Override
	public Predicate getOn() {
		return suppliedJoinCondition;
	}
}
