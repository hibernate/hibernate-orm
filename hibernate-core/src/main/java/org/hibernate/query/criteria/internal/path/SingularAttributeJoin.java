/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.internal.FromImplementor;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

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
		if ( Attribute.PersistentAttributeType.EMBEDDED == joinAttribute.getPersistentAttributeType() ) {
			this.model = (Bindable<X>) joinAttribute;
		}
		else {
			if ( javaType != null ) {
				this.model = (Bindable<X>) criteriaBuilder.getEntityManagerFactory().getMetamodel().managedType( javaType );
			}
			else {
				this.model = (Bindable<X>) joinAttribute.getType();
			}
		}
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

	@Override
	@SuppressWarnings("unchecked")
	protected ManagedType<? super X> locateManagedType() {
		if ( getModel().getBindableType() == Bindable.BindableType.ENTITY_TYPE ) {
			return (ManagedType<? super X>) getModel();
		}
		else if ( getModel().getBindableType() == Bindable.BindableType.SINGULAR_ATTRIBUTE ) {
			final Type joinedAttributeType = ( (SingularAttribute) getAttribute() ).getType();
			if ( !ManagedType.class.isInstance( joinedAttributeType ) ) {
				throw new UnsupportedOperationException(
						"Cannot further dereference attribute join [" + getPathIdentifier() + "] as its type is not a ManagedType"
				);
			}
			return (ManagedType<? super X>) joinedAttributeType;
		}
		else if ( getModel().getBindableType() == Bindable.BindableType.PLURAL_ATTRIBUTE ) {
			final Type elementType = ( (PluralAttribute) getAttribute() ).getElementType();
			if ( !ManagedType.class.isInstance( elementType ) ) {
				throw new UnsupportedOperationException(
						"Cannot further dereference attribute join [" + getPathIdentifier() + "] (plural) as its element type is not a ManagedType"
				);
			}
			return (ManagedType<? super X>) elementType;
		}

		return super.locateManagedType();
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
			return isCorrelated() ? getCorrelationParent().getAlias() : super.getAlias();
		}

		@Override
		public void prepareAlias(RenderingContext renderingContext) {
			if ( getAlias() == null ) {
				if ( isCorrelated() ) {
					setAlias( getCorrelationParent().getAlias() );
				}
				else {
					setAlias( renderingContext.generateAlias() );
				}
			}
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
		public String render(RenderingContext renderingContext) {
			return "treat(" + original.render( renderingContext ) + " as " + treatAsType.getName() + ")";
		}
	}
}
