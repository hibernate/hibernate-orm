/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.serialization.entity.BuildRecord;
import org.hibernate.orm.test.serialization.entity.BuildRecordId;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				BuildRecord.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting( name = Environment.CACHE_REGION_FACTORY, value = "org.hibernate.testing.cache.CachingRegionFactory" ),
				@Setting( name = Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, value = "transactional"),
				@Setting( name = "javax.persistence.sharedCache.mode", value = "ALL"),
				@Setting( name = Environment.CACHE_KEYS_FACTORY, value = "org.hibernate.cache.internal.DefaultCacheKeysFactory" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
@JiraKey("HHH-14843")
public class CacheKeyEmbeddedIdEnanchedTest  {

	@Test
	public void testDefaultCacheKeysFactorySerialization(SessionFactoryScope scope) throws Exception {
		testId( scope, DefaultCacheKeysFactory.INSTANCE, BuildRecord.class.getName(), new BuildRecordId( 2l ) );
	}

	@Test
	public void testSimpleCacheKeysFactorySerialization(SessionFactoryScope scope) throws Exception {
		testId( scope, SimpleCacheKeysFactory.INSTANCE, BuildRecord.class.getName(), new BuildRecordId( 2l ) );
	}

	private void testId(SessionFactoryScope scope, CacheKeysFactory cacheKeysFactory, String entityName, Object id) throws Exception {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister persister = sessionFactory.getMappingMetamodel().findEntityDescriptor( entityName );
		final Object key = cacheKeysFactory.createEntityKey(
				id,
				persister,
				sessionFactory,
				null
		);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream( baos );
		oos.writeObject( key );

		final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
		final Object keyClone = ois.readObject();

		assertEquals( key, keyClone );
		assertEquals( keyClone, key );

		assertEquals( key.hashCode(), keyClone.hashCode() );

		final Object idClone;
		if ( cacheKeysFactory == SimpleCacheKeysFactory.INSTANCE ) {
			idClone = cacheKeysFactory.getEntityId( keyClone );
		}
		else {
			// DefaultCacheKeysFactory#getEntityId will return a disassembled version
			try (Session session = sessionFactory.openSession()) {
				idClone = persister.getIdentifierType().assemble(
						(Serializable) cacheKeysFactory.getEntityId( keyClone ),
						(SharedSessionContractImplementor) session,
						null
				);
			}
		}

		assertEquals( id.hashCode(), idClone.hashCode() );
		assertEquals( id, idClone );
		assertEquals( idClone, id );
		assertTrue( persister.getIdentifierType().isEqual( id, idClone, sessionFactory ) );
		assertTrue( persister.getIdentifierType().isEqual( idClone, id, sessionFactory ) );
	}
}
