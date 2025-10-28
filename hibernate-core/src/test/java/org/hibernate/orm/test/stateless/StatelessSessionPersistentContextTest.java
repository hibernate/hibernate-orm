/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import java.util.Collections;
import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				StatelessSessionPersistentContextTest.TestEntity.class,
				StatelessSessionPersistentContextTest.OtherEntity.class
		}
)
@SessionFactory
public class StatelessSessionPersistentContextTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-13672")
	public void testStatelessSessionPersistenceContextIsCleared(SessionFactoryScope scope) {
		TestEntity testEntity = new TestEntity();
		consumeAndCheckPersistenceContextIsClosed(
				scope,
				statelessSession -> {
					testEntity.setName( "Fab" );
					OtherEntity otherEntity = new OtherEntity();
					otherEntity.setName( "other" );
					testEntity.setOtherEntity( otherEntity );
					statelessSession.insert( otherEntity );
					statelessSession.insert( testEntity );
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				scope,
				statelessSession -> {
					statelessSession.get( TestEntity.class, testEntity.getId() );
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				scope,
				statelessSession -> {
					TestEntity p2 = (TestEntity) statelessSession.get( TestEntity.class, testEntity.getId() );
					p2.setName( "Fabulous" );
					statelessSession.update( p2 );
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				scope,
				statelessSession -> {
					TestEntity testEntity1 = (TestEntity) statelessSession.createQuery(
							"select p from TestEntity p where id = :id" )
							.setParameter( "id", testEntity.getId() )
							.uniqueResult();
					testEntity1.getOtherEntity();
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				scope,
				statelessSession -> {
					statelessSession.refresh( testEntity );
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				scope,
				statelessSession -> {
					statelessSession.delete( testEntity );
				}
		);
	}

	private void consumeAndCheckPersistenceContextIsClosed(
			SessionFactoryScope scope,
			Consumer<StatelessSession> consumer) {
		Transaction transaction = null;
		StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession();
		try {
			transaction = statelessSession.beginTransaction();
			consumer.accept( statelessSession );
			transaction.commit();
		}
		catch (Exception e) {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
			throw e;
		}
		finally {
			statelessSession.close();
		}
		assertThatPersistenContextIsCleared( statelessSession );
	}

	private void assertThatPersistenContextIsCleared(StatelessSession ss) {
		PersistenceContext persistenceContextInternal = ( (SharedSessionContractImplementor) ss ).getPersistenceContextInternal();
		assertTrue(
				persistenceContextInternal.getEntitiesByKey().isEmpty(),
				"StatelessSession: PersistenceContext has not been cleared"
		);
		assertTrue(
				persistenceContextInternal.managedEntitiesIterator() == Collections.emptyIterator(),
				"StatelessSession: PersistenceContext has not been cleared"
		);
		assertTrue(
				persistenceContextInternal.getCollectionsByKey().isEmpty(),
				"StatelessSession: PersistenceContext has not been cleared"
		);
		assertTrue(
				persistenceContextInternal.getCollectionsByKey() == Collections.EMPTY_MAP,
				"StatelessSession: PersistenceContext has not been cleared"
		);
	}

	@Entity(name = "TestEntity")
	@Table(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn
		private OtherEntity otherEntity;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public OtherEntity getOtherEntity() {
			return otherEntity;
		}

		public void setOtherEntity(OtherEntity otherEntity) {
			this.otherEntity = otherEntity;
		}
	}

	@Entity(name = "OtherEntity")
	@Table(name = "OtherEntity")
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
