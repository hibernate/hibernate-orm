/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.org.hierarchyorder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.hibernate.orm.test.mapping.fetch.depth.SysModule;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.Action;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.classloader.ShrinkWrapClassLoader;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.Persistence.createEntityManagerFactory;
import static org.hibernate.cfg.EnvironmentSettings.CLASSLOADERS;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HierarchyOrderTest {

	private EntityManagerFactory emf;
	private DerOA deroa;
	private DerOB derob;

	@BeforeEach
	void setUp() {
		DerDB derba1 = new DerDB( 5 );
		DerDA derda1 = new DerDA( "1", "abase" );
		deroa = new DerOA( derda1 );
		derob = new DerOB( derba1 );
		emf = buildEntityManagerFactory();
	}

	@Test
	void testBaseProperty() {
		try (EntityManager em = emf.createEntityManager()) {
			em.getTransaction().begin();
			em.persist( deroa );
			em.persist( derob );
			em.getTransaction().commit();
			Integer ida = deroa.getId();
			Integer idb = derob.getId();
			em.clear();
			TypedQuery<DerOA> qa = em.createQuery( "select o from DerOA o where o.id =:id", DerOA.class );
			qa.setParameter( "id", ida );
			DerOA deroain = qa.getSingleResult();
			assertEquals( "abase", deroain.derda.baseprop );
		}
	}

	@Test
	void testDerivedProperty() {
		try (EntityManager em = emf.createEntityManager()) {
			em.getTransaction().begin();
			em.persist( deroa );
			em.persist( derob );
			em.getTransaction().commit();
			Integer idb = derob.getId();
			em.clear();

			TypedQuery<DerOB> qb = em.createQuery( "select o from DerOB o where o.id =:id", DerOB.class );
			qb.setParameter( "id", idb );
			DerOB derobin = qb.getSingleResult();
			assertNotNull( derobin );
			assertEquals( 5, derobin.derdb().b );
		}
	}

	private EntityManagerFactory buildEntityManagerFactory() {
		final JavaArchive par = ShrinkWrap.create( JavaArchive.class, "hierarchyorder.par" );
		par.addClasses( SysModule.class );
		par.addAsResource("hierarchyorder/hierarchyorder.xml", "META-INF/persistence.xml" );

		try (final ShrinkWrapClassLoader classLoader = new ShrinkWrapClassLoader( par )) {
			final Map<String, Object> settings = new HashMap<>( Map.of(
					CLASSLOADERS, Arrays.asList( classLoader, getClass().getClassLoader() ),
					HBM2DDL_AUTO, Action.CREATE_DROP,
					FORMAT_SQL, "true"
			) );
			ServiceRegistryUtil.applySettings( settings );

			return createEntityManagerFactory("hierarchyorder", settings );
		}
		catch (IOException e) {
			throw new RuntimeException( "re-throw", e );
		}
	}
}
