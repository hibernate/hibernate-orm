/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.*;

@DomainModel(annotatedClasses = {
		MergeRecordPropertyTestCase.MyEntity.class
})
@SessionFactory
@JiraKey("HHH-16759")
public class MergeRecordPropertyTestCase {

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void mergeDetached(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist( new MyEntity( 1L, new MyRecord( "test", "abc" ) ) )
		);
		scope.inTransaction(
				session -> session.merge( new MyEntity( 1L, new MyRecord( "test", "d" ) ) )
		);
		scope.inSession(
				session -> {
					final MyEntity entity = session.find( MyEntity.class, 1L );
					assertEquals( "test", entity.record.name );
					assertEquals( "d", entity.record.description );
				}
		);
	}

	@Test
	public void mergeDetachedNullComponent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist( new MyEntity( 1L, new MyRecord( "test", "abc" ) ) )
		);
		scope.inTransaction(
				session -> session.merge( new MyEntity( 1L ) )
		);
		scope.inSession(
				session -> {
					final MyEntity entity = session.find( MyEntity.class, 1L );
					assertNull( entity.record );
				}
		);
	}

	@Test
	public void mergeTransient(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.merge( new MyEntity( 1L, new MyRecord( "test", "xyz" ) ) )
		);
		scope.inSession(
				session -> {
					final MyEntity entity = session.find( MyEntity.class, 1L );
					assertEquals( "test", entity.record.name );
					assertEquals( "xyz", entity.record.description );
				}
		);
	}

	@Test
	public void mergeTransientNullComponent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.merge( new MyEntity( 1L ) )
		);
		scope.inSession(
				session -> {
					final MyEntity entity = session.find( MyEntity.class, 1L );
					assertNull( entity.record );
				}
		);
	}

	@Test
	public void mergePersistent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final MyEntity entity = new MyEntity( 1L, new MyRecord( "test", "efg" ) );
					session.persist( entity );
					entity.setRecord( new MyRecord( "test", "h" ) );
					session.merge( entity );
				}
		);
		scope.inSession(
				session -> {
					final MyEntity entity = session.find( MyEntity.class, 1L );
					assertEquals( "test", entity.record.name );
					assertEquals( "h", entity.record.description );
				}
		);
	}

	@Test
	public void mergePersistentDetached(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final MyEntity entity = new MyEntity( 1L, new MyRecord( "test", "abc" ) );
					session.persist( entity );
					session.flush();
					session.clear();
					final MyEntity entity2 = new MyEntity( 2L, new MyRecord( "test", "efg", new MyEntity( 1L ) ) );
					final MyEntity mergedEntity = session.merge( entity2 );
					assertTrue( session.contains( mergedEntity.record.assoc() ) );
				}
		);
	}

	@Test
	public void mergePersistentManaged(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final MyEntity entity = new MyEntity( 1L, new MyRecord( "test", "abc" ) );
					session.persist( entity );
					final MyEntity entity2 = new MyEntity( 1L, new MyRecord( "test", "efg", new MyEntity( 1L ) ) );
					final MyEntity mergedEntity = session.merge( entity2 );
					assertTrue( session.contains( mergedEntity.record.assoc() ) );
				}
		);
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		Long id;
		@Embedded
		MyRecord record;

		public MyEntity() {
		}

		public MyEntity(Long id) {
			this.id = id;
		}

		public MyEntity(Long id, MyRecord record) {
			this.id = id;
			this.record = record;
		}

		public Long getId() {
			return id;
		}

		public MyRecord getRecord() {
			return record;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setRecord(MyRecord record) {
			this.record = record;
		}
	}

	@Embeddable
	public static record MyRecord(String name, String description, @ManyToOne(fetch = FetchType.LAZY) MyEntity assoc) {
		public MyRecord(String name, String description) {
			this( name, description, null );
		}
	}
}
