/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.orm.test.annotations.embeddables.Investor;
import org.hibernate.orm.test.annotations.embeddables.InvestorTypeContributor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This integration test verifies that serialized entities
 * can be deserialized and re-connected to a SessionFactory
 * by using the SessionFactory name rather than its UUID.
 * This is relevant for clustering, as different SessionFactory
 * instances on different nodes will have a different UUID
 * but can be configured to match the name.
 *
 * @author Marcel Overdijk
 */
public class MultipleSessionFactoriesProxyTest {

	private SessionFactory produceSessionFactory() {
		try (InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream( "org/hibernate/orm/test/proxy/DataPoint.hbm.xml" )) {
			final Configuration cfg = new Configuration()
					.addInputStream( is )
					.addAnnotatedClass( Investor.class )
					.registerTypeContributor( new InvestorTypeContributor() )
					.setProperty( Environment.HBM2DDL_AUTO, "create-drop" )
					.setProperty( Environment.SESSION_FACTORY_NAME, "sf-name" )
					.setProperty( Environment.SESSION_FACTORY_NAME, "sf-name" );
			ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
			return cfg.buildSessionFactory();
		}
		catch (IOException e) {
			throw new IllegalArgumentException( e );
		}
	}

	@Test
	@JiraKey(value = "HHH-17172")
	public void testProxySerializationWithMultipleSessionFactories() {
		SessionFactory sf = produceSessionFactory();
		try {
			Container c = sf.fromTransaction(
					session -> {
						Container container = new Container( "container" );
						container.setOwner( new Owner( "owner" ) );
						container.setInfo( new Info( "blah blah blah" ) );
						container.getDataPoints()
								.add( new DataPoint( new BigDecimal( 1 ), new BigDecimal( 1 ), "first data point" ) );
						container.getDataPoints()
								.add( new DataPoint( new BigDecimal( 2 ), new BigDecimal( 2 ), "second data point" ) );
						session.persist( container );
						session.flush();
						session.clear();

						container = session.getReference( Container.class, container.getId() );
						assertThat( Hibernate.isInitialized( container ) ).isFalse();
						container.getId();
						assertThat( Hibernate.isInitialized( container ) ).isFalse();
						container.getName();
						assertThat( Hibernate.isInitialized( container ) ).isTrue();
						return container;
					}
			);
			// Serialize the container.
			byte[] bytes = SerializationHelper.serialize( c );

			// Now deserialize, which works at this stage, because the session factory UUID in the
			// serialized object is the same as the current session factory.
			SerializationHelper.deserialize( bytes );

			// Now rebuild the session factory (it will get a new unique UUID).
			// As configured for this test an explicit session factory name is used ("sf-name"),
			// so we would expect the deserialization to work after rebuilding the session factory.
			// Rebuilding the session factory simulates multiple JVM instances with different session
			// factories (different UUID's, but same name!) trying to deserialize objects from a
			// centralized cache (e.g. Hazelcast).
			sf.close();
			produceSessionFactory();

			// And this should be possible now: even though we lack of a matching session factory by
			// UUID, it should seek a match by using the session factory name as valid alternative.
			SerializationHelper.deserialize( bytes );
		}
		finally {
			sf.close();
		}

	}
}
