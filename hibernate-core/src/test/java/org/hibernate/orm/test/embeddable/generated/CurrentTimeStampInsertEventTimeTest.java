/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.generated;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CurrentTimeStampInsertEventTimeTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-18756")
public class CurrentTimeStampInsertEventTimeTest {

	@Test
	public void testCurrentTimeStamp(SessionFactoryScope scope) {
		Long testEntityId = 1L;
		String name = "a";
		String value = "b";
		Date now = new Date();

		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( testEntityId, name, value );
					testEntity.anEmbeddable.setTimestamp( now );
					session.persist( testEntity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, testEntityId );
					assertThat( testEntity ).isNotNull();
					assertThat( testEntity.getName() ).isEqualTo( name );
					AnEmbeddable anEmbeddable = testEntity.getAnEmbeddable();
					assertThat( anEmbeddable ).isNotNull();
					assertThat( anEmbeddable.getaString() ).isEqualTo( value );
					assertThat( anEmbeddable.getTimestamp() ).isNull();
					anEmbeddable.setTimestamp( now );
				}
		);

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, testEntityId );
			assertThat( testEntity ).isNotNull();
			assertThat( testEntity.getName() ).isEqualTo( name );
			AnEmbeddable anEmbeddable = testEntity.getAnEmbeddable();
			assertThat( anEmbeddable ).isNotNull();
			assertThat( anEmbeddable.getaString() ).isEqualTo( value );
			assertThat( anEmbeddable.getTimestamp() ).isNotNull();
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		@Embedded
		private AnEmbeddable anEmbeddable;

		public TestEntity() {
		}

		public TestEntity(Long id, String name, String aString) {
			this.id = id;
			this.name = name;
			anEmbeddable = new AnEmbeddable( aString );
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public AnEmbeddable getAnEmbeddable() {
			return anEmbeddable;
		}
	}

	public static class AnEmbeddable {
		@CurrentTimestamp(event = EventType.INSERT, source = SourceType.DB)
		@Column(name = "timestamp_column")
		private Date timestamp;

		private String aString;

		public AnEmbeddable() {
		}

		public AnEmbeddable(String aString) {
			this.aString = aString;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Date timestamp) {
			this.timestamp = timestamp;
		}

		public String getaString() {
			return aString;
		}
	}
}
