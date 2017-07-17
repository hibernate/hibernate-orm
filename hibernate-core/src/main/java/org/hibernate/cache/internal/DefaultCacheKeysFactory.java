/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Second level cache providers now have the option to use custom key implementations.
 * This was done as the default key implementation is very generic and is quite
 * a large object to allocate in large quantities at runtime.
 * In some extreme cases, for example when the hit ratio is very low, this was making the efficiency
 * penalty vs its benefits tradeoff questionable.
 * <p/>
 * Depending on configuration settings there might be opportunities to
 * use simpler key implementations, for example when multi-tenancy is not being used to
 * avoid the tenant identifier, or when a cache instance is entirely dedicated to a single type
 * to use the primary id only, skipping the role or entity name.
 * <p/>
 * Even with multiple types sharing the same cache, their identifiers could be of the same
 * {@link JavaTypeDescriptor}; in this case the cache container could
 * use a single type reference to implement a custom equality function without having
 * to look it up on each equality check: that's a small optimisation but the
 * equality function is often invoked extremely frequently.
 * <p/>
 * Another reason is to make it more convenient to implement custom serialization protocols when the
 * implementation supports clustering.
 *
 * @see JavaTypeDescriptor#extractHashCode(Object)
 * @see JavaTypeDescriptor#areEqual(Object, Object)
 * @author Sanne Grinovero
 * @since 5.0
 */
public class DefaultCacheKeysFactory {

	public static Object createCollectionKey(Object id, PersistentCollectionDescriptor descriptor, SessionFactoryImplementor factory, String tenantIdentifier) {
		return new OldCacheKeyImplementation( id, descriptor.getKeyJavaTypeDescriptor(), descriptor.getNavigableRole().getFullPath(), tenantIdentifier );
	}

	public static Object createEntityKey(Object id, EntityDescriptor descriptor, SessionFactoryImplementor factory, String tenantIdentifier) {
		return new OldCacheKeyImplementation(
				id,
				descriptor.getIdentifierType().getJavaTypeDescriptor(),
				descriptor.getHierarchy().getRootEntityType().getEntityName(),
				tenantIdentifier
		);
	}

	public static Object createNaturalIdKey(Object[] naturalIdValues, EntityDescriptor descriptor, SharedSessionContractImplementor session) {
		return new OldNaturalIdCacheKey(
				naturalIdValues,
				descriptor.getPropertyTypes(),
				descriptor.getNaturalIdentifierProperties(),
				descriptor.getHierarchy().getRootEntityType().getEntityName(),
				session
		);
	}

	public static Object getEntityId(Object cacheKey) {
		return ((OldCacheKeyImplementation) cacheKey).getId();
	}

	public static Object getCollectionId(Object cacheKey) {
		return ((OldCacheKeyImplementation) cacheKey).getId();
	}

	public static Object[] getNaturalIdValues(Object cacheKey) {
		return ((OldNaturalIdCacheKey) cacheKey).getNaturalIdValues();
	}

	public static CacheKeysFactory INSTANCE = new CacheKeysFactory() {
		@Override
		public Object createCollectionKey(Object id, PersistentCollectionDescriptor persister, SessionFactoryImplementor factory, String tenantIdentifier) {
			return DefaultCacheKeysFactory.createCollectionKey(id, persister, factory, tenantIdentifier);
		}

		@Override
		public Object createEntityKey(Object id, EntityDescriptor persister, SessionFactoryImplementor factory, String tenantIdentifier) {
			return DefaultCacheKeysFactory.createEntityKey(id, persister, factory, tenantIdentifier);
		}

		@Override
		public Object createNaturalIdKey(Object[] naturalIdValues, EntityDescriptor persister, SharedSessionContractImplementor session) {
			return DefaultCacheKeysFactory.createNaturalIdKey(naturalIdValues, persister, session);
		}

		@Override
		public Object getEntityId(Object cacheKey) {
			return DefaultCacheKeysFactory.getEntityId(cacheKey);
		}

		@Override
		public Object getCollectionId(Object cacheKey) {
			return DefaultCacheKeysFactory.getCollectionId(cacheKey);
		}

		@Override
		public Object[] getNaturalIdValues(Object cacheKey) {
			return DefaultCacheKeysFactory.getNaturalIdValues(cacheKey);
		}
	};
}
