package org.hibernate.orm.test.records;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = MergeRecordEmbeddedIdTest.MyEntity.class )
@SessionFactory
public class MergeRecordEmbeddedIdTest {
	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from MyEntity" ).executeUpdate() );
	}

	@Test
	public void mergeDetached(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		scope.inTransaction( session -> session.persist( new MyEntity( id, "xyz" ) ) );
		scope.inTransaction( session -> session.merge( new MyEntity(
				id,
				"abc",
				new MyEntity( new MyRecord( 2, "entity_2" ) )
		) ) );
		scope.inSession( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( "abc", result.getData() );
			assertEquals( 2, result.getAssociatedEntity().getRecord().code );
			assertEquals( "entity_2", result.getAssociatedEntity().getRecord().description );
		} );
	}

	@Test
	public void mergeDetachedNullAssociation(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		scope.inTransaction( session -> session.persist( new MyEntity( id ) ) );
		scope.inTransaction( session -> session.merge( new MyEntity( id, "abc" ) ) );
		scope.inSession( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( "abc", result.getData() );
			assertNull( result.getAssociatedEntity() );
		} );
	}

	@Test
	public void mergeTransient(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		final MyEntity myEntity = new MyEntity( id, "abc" );
		myEntity.associatedEntity = new MyEntity( new MyRecord( 2, "entity_2" ), "xyz" );
		scope.inTransaction( session -> session.merge( myEntity ) );
		scope.inSession( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( "abc", result.getData() );
			assertEquals( 2, result.getAssociatedEntity().getRecord().code );
			assertEquals( "entity_2", result.getAssociatedEntity().getRecord().description );
			assertEquals( "xyz", result.getAssociatedEntity().getData() );
		} );
	}

	@Test
	public void mergeTransientNullAssociation(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		final MyEntity myEntity = new MyEntity( id, "abc" );
		scope.inTransaction( session -> session.merge( myEntity ) );
		scope.inSession( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( "abc", result.getData() );
			assertNull( result.getAssociatedEntity() );
		} );
	}

	@Test
	public void mergePersistent(SessionFactoryScope scope) {
		final MyRecord id = new MyRecord( 1, "entity_1" );
		scope.inTransaction( session -> {
			final MyEntity entity = new MyEntity( id );
			session.persist( entity );
			entity.data = "abc";
			entity.associatedEntity = new MyEntity( new MyRecord( 2, "entity_2" ) );
			session.merge( entity );
		} );
		scope.inSession( session -> {
			final MyEntity result = session.find( MyEntity.class, id );
			assertEquals( "abc", result.getData() );
			assertEquals( 2, result.getAssociatedEntity().getRecord().code );
			assertEquals( "entity_2", result.getAssociatedEntity().getRecord().description );
		} );
	}

	@Entity( name = "MyEntity" )
	public static class MyEntity {
		@EmbeddedId
		private MyRecord record;

		private String data;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL )
		private MyEntity associatedEntity;

		public MyEntity() {
		}

		public MyEntity(MyRecord record) {
			this( record, null );
		}

		public MyEntity(MyRecord record, String data) {
			this.record = record;
			this.data = data;
		}

		public MyEntity(MyRecord record, String data, MyEntity associatedEntity) {
			this.record = record;
			this.data = data;
			this.associatedEntity = associatedEntity;
		}

		public MyRecord getRecord() {
			return record;
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
