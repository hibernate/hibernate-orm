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
import java.util.Map;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.MapAttribute;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.CriteriaSubqueryImpl;
import org.hibernate.jpa.criteria.FromImplementor;
import org.hibernate.jpa.criteria.MapJoinImplementor;
import org.hibernate.jpa.criteria.PathImplementor;
import org.hibernate.jpa.criteria.PathSource;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.expression.MapEntryExpression;

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
