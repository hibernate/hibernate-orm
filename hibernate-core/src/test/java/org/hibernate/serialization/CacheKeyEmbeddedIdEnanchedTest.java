package org.hibernate.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.serialization.entity.BuildRecord;
import org.hibernate.serialization.entity.BuildRecordId;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
@TestForIssue( jiraKey = "HHH-14843")
public class CacheKeyEmbeddedIdEnanchedTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		settings.put( Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "transactional" );
		settings.put( "javax.persistence.sharedCache.mode", "ALL" );
		settings.put( Environment.CACHE_KEYS_FACTORY, DefaultCacheKeysFactory.INSTANCE.getClass().getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BuildRecord.class };
	}

	@Test
	public void testDefaultCacheKeysFactorySerialization() throws Exception {
		testId( DefaultCacheKeysFactory.INSTANCE, BuildRecord.class.getName(), new BuildRecordId( 2l ) );
	}

	@Test
	public void testSimpleCacheKeysFactorySerialization() throws Exception {
		testId( SimpleCacheKeysFactory.INSTANCE, BuildRecord.class.getName(), new BuildRecordId( 2l ) );
	}

	private void testId(CacheKeysFactory cacheKeysFactory, String entityName, Object id) throws Exception {
		final EntityPersister persister = sessionFactory().getMetamodel().entityPersister( entityName );
		final Object key = cacheKeysFactory.createEntityKey(
				id,
				persister,
				sessionFactory(),
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

		final Object idClone = cacheKeysFactory.getEntityId( keyClone );

		assertEquals( id.hashCode(), idClone.hashCode() );
		assertEquals( id, idClone );
		assertEquals( idClone, id );
		assertTrue( persister.getIdentifierType().isEqual( id, idClone, sessionFactory() ) );
		assertTrue( persister.getIdentifierType().isEqual( idClone, id, sessionFactory() ) );
	}
}
