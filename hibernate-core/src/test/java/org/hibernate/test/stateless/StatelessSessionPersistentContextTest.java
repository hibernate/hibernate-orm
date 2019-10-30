/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stateless;

import java.util.Collections;
import java.util.function.Consumer;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class StatelessSessionPersistentContextTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class, OtherEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13672")
	public void testStatelessSessionPersistenceContextIsCleared() {
		TestEntity testEntity = new TestEntity();
		consumeAndCheckPersistenceContextIsClosed(
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
				statelessSession -> {
					statelessSession.get( TestEntity.class, testEntity.getId() );
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				statelessSession -> {
					TestEntity p2 = (TestEntity) statelessSession.get( TestEntity.class, testEntity.getId() );
					p2.setName( "Fabulous" );
					statelessSession.update( p2 );
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				statelessSession -> {
					TestEntity testEntity1 = (TestEntity) statelessSession.createQuery(
							"select p from TestEntity p where id = :id" )
							.setParameter( "id", testEntity.getId() )
							.uniqueResult();
					testEntity1.getOtherEntity();
				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				statelessSession -> {
					statelessSession.refresh( testEntity );

				}
		);

		consumeAndCheckPersistenceContextIsClosed(
				statelessSession -> {
					statelessSession.delete( testEntity );

				}
		);
	}

	private void consumeAndCheckPersistenceContextIsClosed(Consumer<StatelessSession> consumer) {
		Transaction transaction = null;
		StatelessSession statelessSession = sessionFactory().openStatelessSession();
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
				"StatelessSession: PersistenceContext has not been cleared",
				persistenceContextInternal.getEntitiesByKey().isEmpty()
		);
		assertTrue(
				"StatelessSession: PersistenceContext has not been cleared",
				persistenceContextInternal.managedEntitiesIterator() == Collections.emptyIterator()
		);
		assertTrue(
				"StatelessSession: PersistenceContext has not been cleared",
				persistenceContextInternal.getCollectionsByKey().isEmpty()
		);
		assertTrue(
				"StatelessSession: PersistenceContext has not been cleared",
				persistenceContextInternal.getCollectionsByKey() == Collections.emptyMap()
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
