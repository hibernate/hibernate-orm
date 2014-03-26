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
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.ListAttribute;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.CriteriaSubqueryImpl;
import org.hibernate.jpa.criteria.FromImplementor;
import org.hibernate.jpa.criteria.ListJoinImplementor;
import org.hibernate.jpa.criteria.PathImplementor;
import org.hibernate.jpa.criteria.PathSource;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.expression.ListIndexExpression;

/**
 * Models a join based on a list-style plural association attribute.
 *
 * @author Steve Ebersole
 */
public class ListAttributeJoin<O,E>
		extends PluralAttributeJoinSupport<O, List<E>,E>
		implements ListJoinImplementor<O,E>, Serializable {

	public ListAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<E> javaType,
			PathSource<O> pathSource,
			ListAttribute<? super O, E> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	@Override
	public Expression<Integer> index() {
		return new ListIndexExpression( criteriaBuilder(), this, getAttribute() );
	}

	@Override
	public ListAttribute<? super O, E> getAttribute() {
		return (ListAttribute<? super O, E>) super.getAttribute();
	}

	@Override
	public ListAttribute<? super O, E> getModel() {
		return getAttribute();
	}

	@Override
	public final ListAttributeJoin<O,E> correlateTo(CriteriaSubqueryImpl subquery) {
		return (ListAttributeJoin<O,E>) super.correlateTo( subquery );
	}

	@Override
	protected FromImplementor<O, E> createCorrelationDelegate() {
		return new ListAttributeJoin<O,E>(
				criteriaBuilder(),
				getJavaType(),
				(PathImplementor<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
	}

	@Override
	public ListAttributeJoin<O, E> on(Predicate... restrictions) {
		return (ListAttributeJoin<O, E>) super.on( restrictions );
	}

	@Override
	public ListAttributeJoin<O, E> on(Expression<Boolean> restriction) {
		return (ListAttributeJoin<O, E>) super.on( restriction );
	}

	@Override
	public <T extends E> ListAttributeJoin<O,T> treatAs(Class<T> treatAsType) {
		return new TreatedListAttributeJoin<O,T>( this, treatAsType );
	}

	public static class TreatedListAttributeJoin<O,T> extends ListAttributeJoin<O, T> {
		private final ListAttributeJoin<O, ? super T> original;
		private final Class<T> treatAsType;

		@SuppressWarnings("unchecked")
		public TreatedListAttributeJoin(ListAttributeJoin<O, ? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					treatAsType,
					original.getPathSource(),
					(ListAttribute<? super O, T>) original.getAttribute(),
					original.getJoinType()
			);
			this.original = original;
			this.treatAsType = treatAsType;
		}

		@Override
		public String getAlias() {
			return original.getAlias();
		}

		@Override
		public void prepareAlias(RenderingContext renderingContext) {
			// do nothing...
		}

		@Override
		public String render(RenderingContext renderingContext) {
			return "treat(" + original.render( renderingContext ) + " as " + treatAsType.getName() + ")";
		}

		@Override
		public String getPathIdentifier() {
			return "treat(" + getAlias() + " as " + treatAsType.getName() + ")";
		}

		@Override
		protected PathSource getPathSourceForSubPaths() {
			return this;
		}
	}
}
