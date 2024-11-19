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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				NestedEmbeddableCurrentTimeStampInsertUpdateEventTimeTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-18756")
public class NestedEmbeddableCurrentTimeStampInsertUpdateEventTimeTest {

	@Test
	public void testCurrentTimeStamp(SessionFactoryScope scope) {
		Long testEntityId = 1L;
		String name = "a";
		String value = "b";
		String anotherValue = "c";
		String updatedValue = "b1";
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( testEntityId, name, value, anotherValue );
					session.persist( testEntity );
				}
		);

		Date now = new Date();

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, testEntityId );
					assertThat( testEntity ).isNotNull();
					assertThat( testEntity.getName() ).isEqualTo( name );

					AnEmbeddable anEmbeddable = testEntity.getAnEmbeddable();
					assertThat( anEmbeddable ).isNotNull();
					assertThat( anEmbeddable.getaString() ).isEqualTo( value );
					assertThat( anEmbeddable.getTimestamp() ).isNull();

					AnotherEmbeddable anotherEmbeddable = testEntity.getAnotherEmbeddable();
					assertThat( anotherEmbeddable ).isNotNull();
					assertThat( anotherEmbeddable.getAnotherString() ).isEqualTo( anotherValue );
					assertThat( anotherEmbeddable.getAnotherTimestamp() ).isNull();

					anEmbeddable.setTimestamp( now );
					anEmbeddable.setaString( updatedValue );
					anotherEmbeddable.setAnotherTimestamp( now );
				}
		);

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, testEntityId );
			assertThat( testEntity ).isNotNull();
			assertThat( testEntity.getName() ).isEqualTo( name );

			AnEmbeddable anEmbeddable = testEntity.getAnEmbeddable();
			assertThat( anEmbeddable ).isNotNull();
			assertThat( anEmbeddable.getaString() ).isEqualTo( updatedValue );
			assertThat( anEmbeddable.getTimestamp() ).isNull();

			AnotherEmbeddable anotherEmbeddable = testEntity.getAnotherEmbeddable();
			assertThat( anotherEmbeddable ).isNotNull();
			assertThat( anotherEmbeddable.getAnotherString() ).isEqualTo( anotherValue );
			assertThat( anotherEmbeddable.getAnotherTimestamp() ).isNull();
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

		public TestEntity(Long id, String name, String aString, String anotherString) {
			this.id = id;
			this.name = name;
			anEmbeddable = new AnEmbeddable( aString, anotherString );

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

		public AnotherEmbeddable getAnotherEmbeddable() {
			return anEmbeddable.getAnotherEmbeddable();
		}
	}

	public static class AnEmbeddable {
		@CurrentTimestamp(source = SourceType.DB)
		@Column(name = "timestamp_column")
		private Date timestamp;

		private String aString;

		@Embedded
		private AnotherEmbeddable anotherEmbeddable;

		public AnEmbeddable() {
		}

		public AnEmbeddable(String aString, String anotherString) {
			this.aString = aString;
			anotherEmbeddable = new AnotherEmbeddable( anotherString );
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

		public void setaString(String aString) {
			this.aString = aString;
		}

		public AnotherEmbeddable getAnotherEmbeddable() {
			return anotherEmbeddable;
		}
	}

	public static class AnotherEmbeddable {
		@CurrentTimestamp(source = SourceType.DB)
		@Column(name = "another_timestamp_column")
		private Date anotherTimestamp;

		private String anotherString;

		public AnotherEmbeddable() {
		}

		public AnotherEmbeddable(String aString) {
			this.anotherString = aString;
		}

		public Date getAnotherTimestamp() {
			return anotherTimestamp;
		}

		public void setAnotherTimestamp(Date anotherTimestamp) {
			this.anotherTimestamp = anotherTimestamp;
		}

		public String getAnotherString() {
			return anotherString;
		}

		public void setAnotherString(String anotherString) {
			this.anotherString = anotherString;
		}
	}
}
