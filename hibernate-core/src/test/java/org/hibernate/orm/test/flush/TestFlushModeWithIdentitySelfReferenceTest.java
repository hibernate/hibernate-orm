/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-13042")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class TestFlushModeWithIdentitySelfReferenceTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SelfRefEntity.class, SelfRefEntityWithEmbeddable.class };
	}

	@Test
	public void testFlushModeCommit() throws Exception {
		Session session = openSession();
		try {
			session.setHibernateFlushMode( FlushMode.COMMIT );
			loadAndAssert( session, createAndInsertEntity( session ) );
			loadAndInsert( session, createAndInsertEntityEmbeddable( session ) );
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testFlushModeManual() throws Exception {
		Session session = openSession();
		try {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			loadAndAssert( session, createAndInsertEntity( session ) );
			loadAndInsert( session, createAndInsertEntityEmbeddable( session ) );
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testFlushModeAuto() throws Exception {
		Session session = openSession();
		try {
			session.setHibernateFlushMode( FlushMode.AUTO );
			loadAndAssert( session, createAndInsertEntity( session ) );
			loadAndInsert( session, createAndInsertEntityEmbeddable( session ) );
		}
		finally {
			session.close();
		}
	}

	private SelfRefEntity createAndInsertEntity(Session session) {
		try {
			session.getTransaction().begin();

			SelfRefEntity entity = new SelfRefEntity();
			entity.setSelfRefEntity( entity );
			entity.setData( "test" );

			entity = (SelfRefEntity) session.merge( entity );

			// only during manual flush do we want to force a flush prior to commit
			if ( session.getHibernateFlushMode().equals( FlushMode.MANUAL ) ) {
				session.flush();
			}

			session.getTransaction().commit();

			return entity;
		}
		catch ( Exception e ) {
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
			throw e;
		}
	}

	private SelfRefEntityWithEmbeddable createAndInsertEntityEmbeddable(Session session) {
		try {
			session.getTransaction().begin();

			SelfRefEntityWithEmbeddable entity = new SelfRefEntityWithEmbeddable();
			entity.setData( "test" );

			SelfRefEntityInfo info = new SelfRefEntityInfo();
			info.setSeflRefEntityEmbedded( entity );

			entity.setInfo( info );

			entity = (SelfRefEntityWithEmbeddable) session.merge( entity );

			// only during manual flush do we want to force a flush prior to commit
			if ( session.getHibernateFlushMode().equals( FlushMode.MANUAL ) ) {
				session.flush();
			}

			session.getTransaction().commit();

			return entity;
		}
		catch ( Exception e ) {
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
			throw e;
		}
	}

	private void loadAndAssert(Session session, SelfRefEntity mergedEntity) {
		final SelfRefEntity loadedEntity = session.get( SelfRefEntity.class, mergedEntity.getId() );
		assertNotNull( "Expected to find the merged entity but did not.", loadedEntity );
		assertEquals( "test", loadedEntity.getData() );
		assertNotNull( "Expected a non-null self reference", loadedEntity.getSelfRefEntity() );
	}

	private void loadAndInsert(Session session, SelfRefEntityWithEmbeddable mergedEntity) {
		final SelfRefEntityWithEmbeddable loadedEntity = session.get( SelfRefEntityWithEmbeddable.class, mergedEntity.getId() );
		assertNotNull( "Expected to find the merged entity but did not.", loadedEntity );
		assertEquals( "test", loadedEntity.getData() );
		assertNotNull( "Expected a non-null self reference in embeddable", loadedEntity.getInfo().getSeflRefEntityEmbedded() );
	}

	@Entity(name = "SelfRefEntity")
	public static class SelfRefEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;
		@OneToOne(cascade = CascadeType.ALL, optional = true)
		private SelfRefEntity selfRefEntity;
		private String data;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SelfRefEntity getSelfRefEntity() {
			return selfRefEntity;
		}

		public void setSelfRefEntity(SelfRefEntity selfRefEntity) {
			this.selfRefEntity = selfRefEntity;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity(name = "SelfRefEntityWithEmbeddable")
	public static class SelfRefEntityWithEmbeddable {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;
		@Embedded
		private SelfRefEntityInfo info;
		private String data;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SelfRefEntityInfo getInfo() {
			return info;
		}

		public void setInfo(SelfRefEntityInfo info) {
			this.info = info;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Embeddable
	public static class SelfRefEntityInfo {
		@OneToOne(cascade = CascadeType.ALL, optional = true)
		private SelfRefEntityWithEmbeddable seflRefEntityEmbedded;

		public SelfRefEntityWithEmbeddable getSeflRefEntityEmbedded() {
			return seflRefEntityEmbedded;
		}

		public void setSeflRefEntityEmbedded(SelfRefEntityWithEmbeddable seflRefEntityEmbedded) {
			this.seflRefEntityEmbedded = seflRefEntityEmbedded;
		}
	}
}
