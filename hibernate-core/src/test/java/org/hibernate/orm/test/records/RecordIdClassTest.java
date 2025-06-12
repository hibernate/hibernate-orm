/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = RecordIdClassTest.MyEntity.class )
@SessionFactory
public class RecordIdClassTest {
	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Some dialects (e.g. Firebird) (can) fail for a non-cascaded foreign key depending on actual row order
			// Clear foreign key before delete
			session.createMutationQuery( "update MyEntity set associatedEntity = null" ).executeUpdate();
			session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testPersist(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		scope.inTransaction( session -> session.persist( new MyEntity( id.code, id.description, "xyz" ) ) );
		scope.inTransaction( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( id.code, result.getCode() );
			assertEquals( id.description, result.getDescription() );
			assertEquals( "xyz", result.getData() );
		} );
	}

	@Test
	public void testMergeDetached(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		scope.inTransaction( session -> session.persist( new MyEntity( id.code, id.description, "xyz" ) ) );
		scope.inTransaction( session -> session.merge( new MyEntity(
				id.code,
				id.description,
				"abc",
				new MyEntity(  2, "entity_2" )
		) ) );
		scope.inSession( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( id.code, result.getCode() );
			assertEquals( id.description, result.getDescription() );
			assertEquals( "abc", result.getData() );
			assertEquals( 2, result.getAssociatedEntity().getCode() );
			assertEquals( "entity_2", result.getAssociatedEntity().getDescription() );
		} );
	}

	@Test
	public void testMergeTransient(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		final MyEntity myEntity = new MyEntity( id.code, id.description, "abc" );
		myEntity.associatedEntity = new MyEntity( 2, "entity_2", "xyz" );
		scope.inTransaction( session -> session.merge( myEntity ) );
		scope.inSession( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( id.code, result.getCode() );
			assertEquals( id.description, result.getDescription() );
			assertEquals( "abc", result.getData() );
			assertEquals( 2, result.getAssociatedEntity().getCode() );
			assertEquals( "entity_2", result.getAssociatedEntity().getDescription() );
			assertEquals( "xyz", result.getAssociatedEntity().getData() );
		} );
	}

	@Entity( name = "MyEntity" )
	@IdClass( MyRecord.class )
	public static class MyEntity {
		@Id
		private Integer code;

		@Id
		private String description;

		private String data;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL )
		private MyEntity associatedEntity;

		public MyEntity() {
		}

		public MyEntity(Integer code, String description) {
			this( code, description, null );
		}

		public MyEntity(Integer code, String description, String data) {
			this( code, description, data, null );
		}

		public MyEntity(Integer code, String description, String data, MyEntity associatedEntity) {
			this.code = code;
			this.description = description;
			this.data = data;
			this.associatedEntity = associatedEntity;
		}

		public Integer getCode() {
			return code;
		}

		public String getDescription() {
			return description;
		}

		public String getData() {
			return data;
		}

		public MyEntity getAssociatedEntity() {
			return associatedEntity;
		}
	}

	@Embeddable
	public record MyRecord(Integer code, String description) {
	}
}
