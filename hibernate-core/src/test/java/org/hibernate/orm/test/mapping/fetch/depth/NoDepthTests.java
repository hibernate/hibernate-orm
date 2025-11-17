/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.depth;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.classloader.ShrinkWrapClassLoader;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import static jakarta.persistence.Persistence.createEntityManagerFactory;
import static org.hibernate.cfg.AvailableSettings.CLASSLOADERS;
import static org.hibernate.cfg.AvailableSettings.FORMAT_SQL;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.MAX_FETCH_DEPTH;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-14993" )
public class NoDepthTests {

	@Test
	public void testWithMax() {
		testIt( true );
	}

	@Test
	public void testNoMax() {
		testIt( false );
	}

	private void testIt(boolean configureMaxDepth) {
		try ( final SessionFactoryImplementor sf = buildSessionFactory( configureMaxDepth ) ) {
			inTransaction( sf, (session) -> {
				session.createSelectionQuery( "from SysModule" ).getResultList();
				session.createSelectionQuery( "from SysModule2" ).getResultList();
			} );
		}
	}

	private static SessionFactoryImplementor buildSessionFactory(boolean configureMax) {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( FORMAT_SQL, "true" );
		registryBuilder.applySetting( HBM2DDL_AUTO, Action.CREATE_DROP );

		registryBuilder.applySetting( MAX_FETCH_DEPTH, configureMax ? "10" : "" );

		return new MetadataSources( registryBuilder.build() )
				.addAnnotatedClasses( SysModule.class, SysModule2.class )
				.buildMetadata()
				.buildSessionFactory()
				.unwrap( SessionFactoryImplementor.class );
	}

	@Test
	public void testWithMaxJpa() {
		testItJpa( true );
	}

	@Test
	public void testNoMaxJpa() {
		testItJpa( false );
	}

	private void testItJpa(boolean configureMax) {
		final JavaArchive par = ShrinkWrap.create( JavaArchive.class, "fetch-depth.par" );
		par.addClasses( SysModule.class );
		par.addAsResource( "units/many2many/fetch-depth.xml", "META-INF/persistence.xml" );

		try ( final ShrinkWrapClassLoader classLoader = new ShrinkWrapClassLoader( par ) ) {
			final Map<String, Object> settings = new HashMap<>( Map.of(
					CLASSLOADERS, Arrays.asList( classLoader, getClass().getClassLoader() ),
					MAX_FETCH_DEPTH, configureMax ? "10" : "",
					HBM2DDL_AUTO, Action.CREATE_DROP,
					FORMAT_SQL, "true"
			) );
			ServiceRegistryUtil.applySettings( settings );

			final EntityManagerFactory emf = createEntityManagerFactory( "fetch-depth", settings );
			try ( final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class ) ) {
				// play around with the SF and make sure it is operable
				inTransaction( sf, (s) -> {
					s.createSelectionQuery( "from SysModule" ).list();
					s.createSelectionQuery( "from SysModule2" ).list();
				});
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "re-throw", e );
		}
	}

}
