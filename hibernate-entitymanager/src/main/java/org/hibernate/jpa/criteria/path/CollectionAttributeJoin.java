/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.path;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.CollectionAttribute;

import org.hibernate.jpa.criteria.CollectionJoinImplementor;
import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.CriteriaSubqueryImpl;
import org.hibernate.jpa.criteria.FromImplementor;
import org.hibernate.jpa.criteria.PathImplementor;
import org.hibernate.jpa.criteria.PathSource;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * Models a join based on a plural association attribute.
 *
 * @author Steve Ebersole
 */
public class CollectionAttributeJoin<O,E>
		extends PluralAttributeJoinSupport<O,Collection<E>,E>
		implements CollectionJoinImplementor<O,E>, Serializable {
	public CollectionAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<E> javaType,
			PathSource<O> pathSource,
			CollectionAttribute<? super O, E> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	public final CollectionAttributeJoin<O,E> correlateTo(CriteriaSubqueryImpl subquery) {
		return (CollectionAttributeJoin<O,E>) super.correlateTo( subquery );
	}

	@Override
	public CollectionAttribute<? super O, E> getAttribute() {
		return (CollectionAttribute<? super O, E>) super.getAttribute();
	}

	@Override
	public CollectionAttribute<? super O, E> getModel() {
		return getAttribute();
	}

	@Override
	protected FromImplementor<O, E> createCorrelationDelegate() {
		return new CollectionAttributeJoin<O,E>(
				criteriaBuilder(),
				getJavaType(),
				(PathImplementor<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
	}

	@Override
	public CollectionAttributeJoin<O, E> on(Predicate... restrictions) {
		return (CollectionAttributeJoin<O,E>) super.on( restrictions );
	}

	@Override
	public CollectionAttributeJoin<O, E> on(Expression<Boolean> restriction) {
		return (CollectionAttributeJoin<O,E>) super.on( restriction );
	}

	@Override
	public <T extends E> CollectionAttributeJoin<O,T> treatAs(Class<T> treatAsType) {
		return new TreatedCollectionAttributeJoin<O,T>( this, treatAsType );
	}

	public static class TreatedCollectionAttributeJoin<O,T> extends CollectionAttributeJoin<O, T> {
		private final CollectionAttributeJoin<O, ? super T> original;
		private final Class<T> treatAsType;

		@SuppressWarnings("unchecked")
		public TreatedCollectionAttributeJoin(CollectionAttributeJoin<O, ? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					treatAsType,
					original.getPathSource(),
					(CollectionAttribute<? super O, T>) original.getAttribute(),
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
