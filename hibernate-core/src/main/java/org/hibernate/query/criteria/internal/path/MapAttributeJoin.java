/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import java.util.Map;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.internal.FromImplementor;
import org.hibernate.query.criteria.internal.MapJoinImplementor;
import org.hibernate.query.criteria.internal.PathImplementor;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.MapEntryExpression;

/**
 * Models a join based on a map-style plural association attribute.
 *
 * @author Steve Ebersole
 */
public class MapAttributeJoin<O,K,V>
		extends PluralAttributeJoinSupport<O, Map<K,V>, V>
		implements MapJoinImplementor<O,K,V>, Serializable {

	public MapAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<V> javaType,
			PathSource<O> pathSource,
			MapAttribute<? super O, K, V> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	@Override
	public MapAttribute<? super O, K, V> getAttribute() {
		return (MapAttribute<? super O, K, V>) super.getAttribute();
	}

	@Override
	public MapAttribute<? super O, K, V> getModel() {
		return getAttribute();
	}

	@Override
	public final MapAttributeJoin<O,K,V> correlateTo(CriteriaSubqueryImpl subquery) {
		return (MapAttributeJoin<O,K,V>) super.correlateTo( subquery );
	}

	@Override
	protected FromImplementor<O, V> createCorrelationDelegate() {
		return new MapAttributeJoin<O,K,V>(
				criteriaBuilder(),
				getJavaType(),
				(PathImplementor<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
	}


	@Override
	public Path<V> value() {
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Expression<Map.Entry<K, V>> entry() {
		return new MapEntryExpression( criteriaBuilder(), Map.Entry.class, this, getAttribute() );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Path<K> key() {
		final MapKeyHelpers.MapKeySource<K,V> mapKeySource = new MapKeyHelpers.MapKeySource<K,V>(
				criteriaBuilder(),
				getAttribute().getJavaType(),
				this,
				getAttribute()
		);
		final MapKeyHelpers.MapKeyAttribute mapKeyAttribute = new MapKeyHelpers.MapKeyAttribute( criteriaBuilder(), getAttribute() );
		return new MapKeyHelpers.MapKeyPath( criteriaBuilder(), mapKeySource, mapKeyAttribute );
	}

	@Override
	public MapJoinImplementor<O, K, V> on(Predicate... restrictions) {
		return (MapJoinImplementor<O, K, V>) super.on( restrictions );
	}

	@Override
	public MapJoinImplementor<O, K, V> on(Expression<Boolean> restriction) {
		return (MapJoinImplementor<O, K, V>) super.on( restriction );
	}

	@Override
	public <T extends V> MapAttributeJoin<O, K, T> treatAs(Class<T> treatAsType) {
		return new TreatedMapAttributeJoin<O,K,T>( this, treatAsType );
	}

	public static class TreatedMapAttributeJoin<O, K, T> extends MapAttributeJoin<O, K, T> {
		private final MapAttributeJoin<O, K, ? super T> original;
		protected final Class<T> treatAsType;

		@SuppressWarnings("unchecked")
		public TreatedMapAttributeJoin(MapAttributeJoin<O, K, ? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					treatAsType,
					original.getPathSource(),
					(MapAttribute<? super O, K, T>) original.getAttribute(),
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
		public String render(RenderingContext renderingContext) {
			return "treat(" + original.render( renderingContext ) + " as " + treatAsType.getName() + ")";
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
		protected PathSource getPathSourceForSubPaths() {
			return this;
		}
	}
}
