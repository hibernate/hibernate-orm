/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.path;

import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.CriteriaSubqueryImpl;
import org.hibernate.jpa.criteria.FromImplementor;
import org.hibernate.jpa.criteria.PathSource;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * Models a join based on a singular attribute
 *
 * @param <O> Represents the parameterized type of the attribute owner
 * @param <X> Represents the parameterized type of the attribute
 *
 * @author Steve Ebersole
 */
public class SingularAttributeJoin<O,X> extends AbstractJoinImpl<O,X> {
	private final Bindable<X> model;

	@SuppressWarnings({ "unchecked" })
	public SingularAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathSource<O> pathSource,
			SingularAttribute<? super O, ?> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
		this.model = (Bindable<X>) (
				Attribute.PersistentAttributeType.EMBEDDED == joinAttribute.getPersistentAttributeType()
						? joinAttribute
						: javaType != null
						? criteriaBuilder.getEntityManagerFactory().getMetamodel().managedType( javaType )
						: joinAttribute.getType()
		);
	}

	@Override
	public SingularAttribute<? super O, ?> getAttribute() {
		return (SingularAttribute<? super O, ?>) super.getAttribute();
	}

	@Override
	public SingularAttributeJoin<O, X> correlateTo(CriteriaSubqueryImpl subquery) {
		return (SingularAttributeJoin<O, X>) super.correlateTo( subquery );
	}

	@Override
	protected FromImplementor<O, X> createCorrelationDelegate() {
		return new SingularAttributeJoin<O,X>(
				criteriaBuilder(),
				getJavaType(),
				getPathSource(),
				getAttribute(),
				getJoinType()
		);
	}

	@Override
	protected boolean canBeJoinSource() {
		return true;
	}

	public Bindable<X> getModel() {
		return model;
	}

	@Override
	public <T extends X> SingularAttributeJoin<O,T> treatAs(Class<T> treatAsType) {
		return new TreatedSingularAttributeJoin<O,T>( this, treatAsType );
	}

	public static class TreatedSingularAttributeJoin<O,T> extends SingularAttributeJoin<O, T> {
		private final SingularAttributeJoin<O, ? super T> original;
		private final Class<T> treatAsType;

		public TreatedSingularAttributeJoin(SingularAttributeJoin<O, ? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					treatAsType,
					original.getPathSource(),
					original.getAttribute(),
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
	}
}
