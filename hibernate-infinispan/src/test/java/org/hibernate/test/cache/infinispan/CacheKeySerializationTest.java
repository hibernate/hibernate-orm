/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Rule;
import org.junit.Test;

import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.test.cache.infinispan.functional.entities.WithEmbeddedId;
import org.hibernate.test.cache.infinispan.functional.entities.PK;
import org.hibernate.test.cache.infinispan.functional.entities.WithSimpleId;
import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class CacheKeySerializationTest extends BaseUnitTestCase {
	@Rule
	public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	private SessionFactoryImplementor getSessionFactory(String cacheKeysFactory) {
		Configuration configuration = new Configuration()
				.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true")
				.setProperty(Environment.CACHE_REGION_FACTORY, TestInfinispanRegionFactory.class.getName())
				.setProperty(Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "transactional")
				.setProperty( AvailableSettings.SHARED_CACHE_MODE, "ALL")
				.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
		if (cacheKeysFactory != null) {
			configuration.setProperty(Environment.CACHE_KEYS_FACTORY, cacheKeysFactory);
		}
		configuration.addAnnotatedClass( WithSimpleId.class );
		configuration.addAnnotatedClass( WithEmbeddedId.class );
		return (SessionFactoryImplementor) configuration.buildSessionFactory();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11202")
	public void testSimpleCacheKeySimpleId() throws Exception {
		testId( SimpleCacheKeysFactory.INSTANCE, WithSimpleId.class.getName(), 1L );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11202")
	public void testSimpleCacheKeyEmbeddedId() throws Exception {
		testId( SimpleCacheKeysFactory.INSTANCE, WithEmbeddedId.class.getName(), new PK( 1L ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11202")
	public void testDefaultCacheKeySimpleId() throws Exception {
		testId( DefaultCacheKeysFactory.INSTANCE, WithSimpleId.class.getName(), 1L  );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11202")
	public void testDefaultCacheKeyEmbeddedId() throws Exception {
		testId( DefaultCacheKeysFactory.INSTANCE, WithEmbeddedId.class.getName(), new PK( 1L ) );
	}

	private void testId(CacheKeysFactory cacheKeysFactory, String entityName, Object id) throws Exception {
		final SessionFactoryImplementor sessionFactory = getSessionFactory( cacheKeysFactory.getClass().getName() );
		final EntityPersister persister = sessionFactory.getEntityPersister( entityName );
		final Object key = cacheKeysFactory.createEntityKey(
				id,
				persister,
				sessionFactory,
				null
		);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject( key );

		final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
		final Object keyClone = ois.readObject();

		try {
			assertEquals( key, keyClone );
			assertEquals( keyClone, key );

			assertEquals( key.hashCode(), keyClone.hashCode() );

			final Object idClone = cacheKeysFactory.getEntityId( keyClone );

			assertEquals( id.hashCode(), idClone.hashCode() );
			assertEquals( id, idClone );
			assertEquals( idClone, id );
			assertTrue( persister.getIdentifierType().isEqual( id, idClone, sessionFactory ) );
			assertTrue( persister.getIdentifierType().isEqual( idClone, id, sessionFactory ) );
			sessionFactory.close();
		}
		finally {
			sessionFactory.close();
		}
	}
}
