/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey( "HHH-15072" )
@DomainModel(annotatedClasses = { OutOfTheBoxRecordAsEmbeddableTest.MyEntity.class})
@SessionFactory
public class OutOfTheBoxRecordAsEmbeddableTest {

	@Test
	public void testRecordPersistLoadAndMerge(SessionFactoryScope scope)  {
		scope.inTransaction(
				session -> {
					session.persist( new MyEntity( 1L, new MyRecord( "test", "abc" ) ) );
				}
		);

		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.get( MyEntity.class, 1L );
					assertNotNull( myEntity );
					assertEquals( "test", myEntity.getRecord().name() );
					assertEquals( "abc", myEntity.getRecord().description() );

					myEntity.setRecord( new MyRecord( "test2", "def" ) );
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
	public static record MyRecord(String name, String description) {}
}
