/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SetAttribute;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.JpaFromImplementor;
import org.hibernate.query.criteria.JpaPathImplementor;
import org.hibernate.query.criteria.JpaPathSourceImplementor;
import org.hibernate.query.criteria.JpaSetJoinImplementor;

/**
 * Models a join based on a set-style plural association attribute.
 *
 * @param <O> Represents the parameterized type of the set owner
 * @param <E> Represents the parameterized type of the set elements
 *
 * @author Steve Ebersole
 */
public class SetAttributeJoin<O,E>
		extends PluralAttributeJoinSupport<O, Set<E>,E>
		implements JpaSetJoinImplementor<O,E>, Serializable {

	public SetAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<E> javaType,
			JpaPathSourceImplementor<O> pathSource,
			SetAttribute<? super O, E> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	@Override
	public SetAttribute<? super O, E> getAttribute() {
		return (SetAttribute<? super O, E>) super.getAttribute();
	}

	@Override
	public SetAttribute<? super O, E> getModel() {
		return getAttribute();
	}

	@Override
	public final SetAttributeJoin<O,E> correlateTo(CriteriaSubqueryImpl subquery) {
		return (SetAttributeJoin<O,E>) super.correlateTo( subquery );
	}

	@Override
	protected JpaFromImplementor<O, E> createCorrelationDelegate() {
		return new SetAttributeJoin<O,E>(
				criteriaBuilder(),
				getJavaType(),
				(JpaPathImplementor<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
	}

	@Override
	public JpaSetJoinImplementor<O, E> on(Predicate... restrictions) {
		return (JpaSetJoinImplementor<O, E>) super.on( restrictions );
	}

	@Override
	public JpaSetJoinImplementor<O, E> on(Expression<Boolean> restriction) {
		return (JpaSetJoinImplementor<O, E>) super.on( restriction );
	}

	@Override
	public <T extends E> SetAttributeJoin<O,T> treatAs(Class<T> treatAsType) {
		return new TreatedSetAttributeJoin<O,T>( this, treatAsType );
	}

	public static class TreatedSetAttributeJoin<O,T> extends SetAttributeJoin<O, T> {
		private final SetAttributeJoin<O, ? super T> original;
		private final Class<T> treatAsType;

		@SuppressWarnings("unchecked")
		public TreatedSetAttributeJoin(SetAttributeJoin<O, ? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					treatAsType,
					original.getPathSource(),
					(SetAttribute<? super O, T>) original.getAttribute(),
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
