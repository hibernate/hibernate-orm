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
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-13042")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(annotatedClasses = {
		TestFlushModeWithIdentitySelfReferenceTest.SelfRefEntity.class,
		TestFlushModeWithIdentitySelfReferenceTest.SelfRefEntityWithEmbeddable.class
})
@SessionFactory
public class TestFlushModeWithIdentitySelfReferenceTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testFlushModeCommit(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			session.setHibernateFlushMode( FlushMode.COMMIT );
			loadAndAssert( session, createAndInsertEntity( session ) );
			loadAndInsert( session, createAndInsertEntityEmbeddable( session ) );
		} );
	}

	@Test
	public void testFlushModeManual(SessionFactoryScope  factoryScope) {
		factoryScope.inSession( (session) -> {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			loadAndAssert( session, createAndInsertEntity( session ) );
			loadAndInsert( session, createAndInsertEntityEmbeddable( session ) );
		} );
	}

	@Test
	public void testFlushModeAuto(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			session.setHibernateFlushMode( FlushMode.AUTO );
			loadAndAssert( session, createAndInsertEntity( session ) );
			loadAndInsert( session, createAndInsertEntityEmbeddable( session ) );
		} );
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
		final SelfRefEntity loadedEntity = session.find( SelfRefEntity.class, mergedEntity.getId() );
		Assertions.assertNotNull( loadedEntity, "Expected to find the merged entity but did not." );
		Assertions.assertEquals( "test", loadedEntity.getData() );
		Assertions.assertNotNull( loadedEntity.getSelfRefEntity(), "Expected a non-null self reference" );
	}

	private void loadAndInsert(Session session, SelfRefEntityWithEmbeddable mergedEntity) {
		final SelfRefEntityWithEmbeddable loadedEntity = session.find( SelfRefEntityWithEmbeddable.class, mergedEntity.getId() );
		Assertions.assertNotNull( loadedEntity, "Expected to find the merged entity but did not." );
		Assertions.assertEquals( "test", loadedEntity.getData() );
		Assertions.assertNotNull( loadedEntity.getInfo().getSeflRefEntityEmbedded(),
				"Expected a non-null self reference in embeddable" );
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
