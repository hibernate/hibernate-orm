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
package org.hibernate.ejb.criteria.path;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.Map;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.MapJoinImplementor;
import org.hibernate.ejb.criteria.PathImplementor;
import org.hibernate.ejb.criteria.PathSource;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * {@link MapJoin#key} poses a number of implementation difficulties in terms of the type signatures
 * amongst the {@link Path}, {@link Join} and {@link Attribute} reference at play.  The implementations found here
 * provide that bridge.
 *
 * @author Steve Ebersole
 */
public class MapKeyHelpers {

	/**
	 * Models a path to a map key.  This is the actual return used from {@link MapJoin#key}
	 *
	 * @param <K> The type of the map key.
	 */
	public static class MapKeyPath<K>
			extends AbstractPathImpl<K>
			implements PathImplementor<K>, Serializable {

		private final MapKeyAttribute<K> mapKeyAttribute;

		public MapKeyPath(
				CriteriaBuilderImpl criteriaBuilder,
				MapKeySource<K,?> source,
				MapKeyAttribute<K> mapKeyAttribute) {
			super( criteriaBuilder, mapKeyAttribute.getJavaType(), source );
			this.mapKeyAttribute = mapKeyAttribute;
		}

		@Override
		public MapKeySource getPathSource() {
			return (MapKeySource) super.getPathSource();
		}
		@Override
		public MapKeyAttribute<K> getAttribute() {
			return mapKeyAttribute;
		}

		private boolean isBasicTypeKey() {
			return Attribute.PersistentAttributeType.BASIC ==
					mapKeyAttribute.getPersistentAttributeType();
		}

		@Override
		protected boolean canBeDereferenced() {
			return ! isBasicTypeKey();
		}

		@Override
		public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
			PathSource<?> source = getPathSource();
			String name;
			if ( source != null ) {
				source.prepareAlias( renderingContext );
				name = source.getPathIdentifier();
			}
			else {
				name = getAttribute().getName();
			}
			return "key(" + name + ")";
		}

		@Override
		protected Attribute locateAttributeInternal(String attributeName) {
			if ( ! canBeDereferenced() ) {
				throw new IllegalArgumentException(
						"Map key [" + getPathSource().getPathIdentifier() + "] cannot be dereferenced"
				);
			}
			throw new UnsupportedOperationException( "Not yet supported!" );
		}
		@Override
		public Bindable<K> getModel() {
			return mapKeyAttribute;
		}
	}

	/**
	 * Defines a {@link Path} for the map which can then be used to represent the source of the
	 * map key "attribute".
	 *
	 * @param <K> The map key type
	 * @param <V> The map value type
	 */
	public static class MapKeySource<K,V>
			extends AbstractPathImpl<Map<K, V>>
			implements PathImplementor<Map<K, V>>, Serializable {

		private final MapAttribute<?,K,V> mapAttribute;
		private final MapJoinImplementor<?,K,V> mapJoin;

		public MapKeySource(
				CriteriaBuilderImpl criteriaBuilder,
				Class<Map<K, V>> javaType,
				MapJoinImplementor<?,K,V> mapJoin,
				MapAttribute<?,K,V> attribute) {
			super( criteriaBuilder, javaType, null );
			this.mapJoin = mapJoin;
			this.mapAttribute = attribute;
		}
		@Override
		public MapAttribute<?,K,V> getAttribute() {
			return mapAttribute;
		}

		@SuppressWarnings({ "unchecked" })
		@Override
		public Bindable<Map<K, V>> getModel() {
			// TODO : ok???  the attribute is in fact bindable, but its type signature is different
			return (Bindable<Map<K, V>>) mapAttribute;
		}

		@Override
		public PathImplementor<?> getParentPath() {
			return (PathImplementor<?>) mapJoin.getParentPath();
		}

		@Override
		public String getPathIdentifier() {
			return mapJoin.getPathIdentifier();
		}

		@Override
		protected boolean canBeDereferenced() {
			return false;
		}

		@Override
		protected Attribute locateAttributeInternal(String attributeName) {
			throw new IllegalArgumentException( "Map [" + mapJoin.getPathIdentifier() + "] cannot be dereferenced" );
		}

	}

	/**
	 * Disallow instantiation
	 */
	private MapKeyHelpers() {
	}

	/**
	 * Defines an {@link javax.persistence.metamodel.Attribute} modelling of a map-key.
	 *
	 * @param <K> The type of the map key
	 */
	public static class MapKeyAttribute<K>
			implements SingularAttribute<Map<K,?>,K>, Bindable<K>, Serializable {
		private final MapAttribute<?,K,?> attribute;
		private final CollectionPersister mapPersister;
		private final org.hibernate.type.Type mapKeyType;
		private final Type<K> jpaType;
		private final BindableType jpaBindableType;
		private final Class<K> jpaBinableJavaType;
		private final PersistentAttributeType persistentAttributeType;

		public MapKeyAttribute(CriteriaBuilderImpl criteriaBuilder, MapAttribute<?, K, ?> attribute) {
			this.attribute = attribute;
			this.jpaType = attribute.getKeyType();
			this.jpaBinableJavaType = attribute.getKeyJavaType();
			this.jpaBindableType = Type.PersistenceType
					.ENTITY.equals( jpaType.getPersistenceType() )
					? BindableType.ENTITY_TYPE
					: BindableType.SINGULAR_ATTRIBUTE;

			String guessedRoleName = determineRole( attribute );
			SessionFactoryImplementor sfi = criteriaBuilder.getEntityManagerFactory().getSessionFactory();
			mapPersister = sfi.getCollectionPersister( guessedRoleName );
			if ( mapPersister == null ) {
				throw new IllegalStateException( "Could not locate collection persister [" + guessedRoleName + "]" );
			}
			mapKeyType = mapPersister.getIndexType();
			if ( mapKeyType == null ) {
				throw new IllegalStateException( "Could not determine map-key type [" + guessedRoleName + "]" );
			}

			this.persistentAttributeType = mapKeyType.isEntityType()
					? PersistentAttributeType.MANY_TO_ONE
					: mapKeyType.isComponentType()
							? PersistentAttributeType.EMBEDDED
							: PersistentAttributeType.BASIC;
		}

		private String determineRole(MapAttribute<?,K,?> attribute) {
			return attribute.getDeclaringType().getJavaType().getName() +
					'.' + attribute.getName();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getName() {
			// TODO : ???
			return "map-key";
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public PersistentAttributeType getPersistentAttributeType() {
			return persistentAttributeType;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ManagedType<Map<K, ?>> getDeclaringType() {
			// TODO : ???
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Class<K> getJavaType() {
			return attribute.getKeyJavaType();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Member getJavaMember() {
			// TODO : ???
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isAssociation() {
			return mapKeyType.isEntityType();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isCollection() {
			return false;
		}
		@Override
		public boolean isId() {
			return false;
		}
		@Override
		public boolean isVersion() {
			return false;
		}
		@Override
		public boolean isOptional() {
			return false;
		}
		@Override
		public Type<K> getType() {
			return jpaType;
		}
		@Override
		public BindableType getBindableType() {
			return jpaBindableType;
		}
		@Override
		public Class<K> getBindableJavaType() {
			return jpaBinableJavaType;
		}
	}
}
