/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.ejb.criteria;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable.BindableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type.PersistenceType;
import org.hibernate.ejb.criteria.JoinImplementors.JoinImplementor;
import org.hibernate.ejb.criteria.expression.ExpressionImpl;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * {@link MapJoin} defines a number of methods which require {@link Path}, {@link Join} and {@link Attribute}
 * implementations which do not fit nicely into the generic signatures it defines for everything else (mainly
 * in terms of dealing with the map key).  The implementations found here provide that bridge.
 *
 * @author Steve Ebersole
 */
public class MapKeyHelpers {

	/**
	 * Represents a join to the key of a map attribute.  Obviously the map key must be an
	 * entity or component type.
	 *
	 * @param <K> The type of the map key
	 * @param <V> The type of the map value
	 */
	public static class MapKeyJoin<K,V> extends JoinImpl<Map<K, V>, K> implements Join<Map<K, V>, K> {
		public MapKeyJoin(QueryBuilderImpl queryBuilder, MapPath<K,V> source, MapKeyAttribute<K> attribute, JoinType jt) {
			super(
					queryBuilder,
					attribute.getJavaType(),
					source,
					attribute,
					jt
			);
		}

		@Override
		public JoinImplementor<Map<K, V>, K> correlateTo(CriteriaSubqueryImpl subquery) {
			throw new UnsupportedOperationException( "Map key join cannot be used as a correlation" );
		}

	}

	/**
	 * Models a path to a map key.
	 *
	 * @param <K> The type of the map key.
	 */
	public static class MapKeyPath<K> extends PathImpl<K> implements Path<K> {
		public MapKeyPath(
				QueryBuilderImpl queryBuilder,
				MapPath<K,?> source,
				MapKeyAttribute<K> attribute) {
			super( queryBuilder, attribute.getJavaType(), source, attribute, attribute.getType() );
		}
	}

	/**
	 * Defines a {@link Path} resulting in a map attribute.  This can then be used as the
	 * parent/origin/source for referencing the map-key.
	 *
	 * @param <K> The map key type
	 * @param <V> The map value type
	 */
	public static class MapPath<K,V> extends PathImpl<Map<K, V>> implements Path<Map<K, V>> {
		private final MapJoin<?,K,V> mapJoin;

		public MapPath(
				QueryBuilderImpl queryBuilder,
				Class<Map<K, V>> javaType,
				MapJoin<?,K,V> mapJoin,
				MapAttribute<?,K,V> attribute,
				Object model) {
			super(queryBuilder, javaType, null, attribute, model);
			this.mapJoin = mapJoin;
		}

		@Override
		public MapAttribute<?,K,V> getAttribute() {
			return (MapAttribute<?,K,V>) super.getAttribute();
		}

		@Override
		public PathImpl<?> getParentPath() {
			return (PathImpl<?>) mapJoin;
		}

	}

	/**
	 * Defines an {@link Attribute} modelling of a map-key.
	 * <p/>
	 * TODO : Ideally something like this needs to be part of the metamodel package
	 *
	 * @param <K> The type of the map key
	 */
	public static class MapKeyAttribute<K> implements SingularAttribute<Map<K,?>,K> {
		private final MapAttribute<?,K,?> attribute;
		private final CollectionPersister mapPersister;
		private final Type mapKeyType;
		private final javax.persistence.metamodel.Type<K> jpaType;
		private final BindableType jpaBindableType;
		private final Class<K> jpaBinableJavaType;

		public MapKeyAttribute(QueryBuilderImpl queryBuilder, MapAttribute<?, K, ?> attribute) {
			this.attribute = attribute;
			this.jpaType = attribute.getKeyType();
			this.jpaBinableJavaType = attribute.getKeyJavaType();
			this.jpaBindableType = PersistenceType.ENTITY.equals( jpaType.getPersistenceType() )
					? BindableType.ENTITY_TYPE
					: BindableType.SINGULAR_ATTRIBUTE;

			String guessedRoleName = determineRole( attribute );
			SessionFactoryImplementor sfi = (SessionFactoryImplementor)
					queryBuilder.getEntityManagerFactory().getSessionFactory();
			mapPersister = sfi.getCollectionPersister( guessedRoleName );
			if ( mapPersister == null ) {
				throw new IllegalStateException( "Could not locate collection persister [" + guessedRoleName + "]" );
			}
			mapKeyType = mapPersister.getIndexType();
			if ( mapKeyType == null ) {
				throw new IllegalStateException( "Could not determine map-key type [" + guessedRoleName + "]" );
			}
		}

		private String determineRole(MapAttribute<?,K,?> attribute) {
			return attribute.getDeclaringType().getJavaType().getName() +
					'.' + attribute.getName();
		}

		/**
		 * {@inheritDoc}
		 */
		public String getName() {
			// TODO : ???
			return "map-key";
		}

		/**
		 * {@inheritDoc}
		 */
		public PersistentAttributeType getPersistentAttributeType() {
			if ( mapKeyType.isEntityType() ) {
				return PersistentAttributeType.MANY_TO_ONE;
			}
			else if ( mapKeyType.isComponentType() ) {
				return PersistentAttributeType.EMBEDDED;
			}
			else {
				return PersistentAttributeType.BASIC;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public ManagedType<Map<K, ?>> getDeclaringType() {
			// TODO : ???
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public Class<K> getJavaType() {
			return attribute.getKeyJavaType();
		}

		/**
		 * {@inheritDoc}
		 */
		public Member getJavaMember() {
			// TODO : ???
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isAssociation() {
			return mapKeyType.isEntityType();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isCollection() {
			return false;
		}

		public boolean isId() {
			return false;
		}

		public boolean isVersion() {
			return false;
		}

		public boolean isOptional() {
			return false;
		}

		public javax.persistence.metamodel.Type<K> getType() {
			return jpaType;
		}

		public BindableType getBindableType() {
			return jpaBindableType;
		}

		public Class<K> getBindableJavaType() {
			return jpaBinableJavaType;
		}
	}

	public static class MapEntryExpression<K,V>
			extends ExpressionImpl<Map.Entry<K,V>>
			implements Expression<Map.Entry<K,V>> {
		private final MapAttribute<?, K, V> attribute;

		public MapEntryExpression(
				QueryBuilderImpl queryBuilder,
				Class<Entry<K, V>> javaType,
				MapAttribute<?, K, V> attribute) {
			super(queryBuilder, javaType);
			this.attribute = attribute;
		}

		public MapAttribute<?, K, V> getAttribute() {
			return attribute;
		}

		public void registerParameters(ParameterRegistry registry) {
			// none to register
		}

	}

	/**
	 * Disallow instantiation
	 */
	private MapKeyHelpers() {
	}

}
