/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.query.criteria.JpaCollectionJoinImplementor;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.JpaFromImplementor;
import org.hibernate.query.criteria.JpaPathImplementor;
import org.hibernate.query.criteria.JpaPathSourceImplementor;

/**
 * Models a join based on a plural association attribute.
 *
 * @author Steve Ebersole
 */
public class CollectionAttributeJoin<O,E>
		extends PluralAttributeJoinSupport<O,Collection<E>,E>
		implements JpaCollectionJoinImplementor<O,E>, Serializable {
	public CollectionAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<E> javaType,
			JpaPathSourceImplementor<O> pathSource,
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
	protected JpaFromImplementor<O, E> createCorrelationDelegate() {
		return new CollectionAttributeJoin<O,E>(
				criteriaBuilder(),
				getJavaType(),
				(JpaPathImplementor<O>) getParentPath(),
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
			return isCorrelated() ? getCorrelationParent().getAlias() : super.getAlias();
		}

		@Override
		protected void setAlias(String alias) {
			super.setAlias( alias );
			original.setAlias( alias );
		}

		@Override
		protected ManagedType<T> locateManagedType() {
			return criteriaBuilder().getEntityManagerFactory().getMetamodel().managedType( treatAsType );
		}

		@Override
		public String getPathIdentifier() {
			return "treat(" + getAlias() + " as " + treatAsType.getName() + ")";
		}

		@Override
		protected JpaPathSourceImplementor getPathSourceForSubPaths() {
			return this;
		}
	}
}
