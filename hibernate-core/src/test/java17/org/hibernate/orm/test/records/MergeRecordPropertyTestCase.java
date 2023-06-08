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

@DomainModel(annotatedClasses = {
		MergeRecordPropertyTestCase.MyEntity.class
})
@SessionFactory
@JiraKey("HHH-16759")
public class MergeRecordPropertyTestCase {

	@Test
	public void merge(SessionFactoryScope scope) {
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
	public static record MyRecord(String name, String description) {
	}
}
