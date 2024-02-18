package org.hibernate.orm.test.records;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		RecordEmbeddedIdMergeTest.SystemUser.class,
		RecordEmbeddedIdMergeTest.PK.class,
})
@SessionFactory
@JiraKey("HHH-17747")
public class RecordEmbeddedIdMergeTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SystemUser" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final SystemUser _systemUser = scope.fromTransaction( session -> {
			final SystemUser systemUser = new SystemUser();
			systemUser.setId( new PK( "mbladel", 42 ) );
			systemUser.setName( "Marco Belladeli" );
			session.persist( systemUser );
			return systemUser;
		} );

		scope.inTransaction( session -> {
			final SystemUser systemUser = session.find( SystemUser.class, _systemUser.getId() );
			final SystemUser systemUser1 = new SystemUser();
			systemUser1.setId( systemUser.getId() );
			systemUser1.setName( "Marco Belladelli" );
			session.merge( systemUser1 );
		} );

		scope.inSession( session -> {
			final SystemUser systemUser = session.find( SystemUser.class, _systemUser.getId() );
			assertThat( systemUser.getName() ).isEqualTo( "Marco Belladelli" );
			assertThat( systemUser.getId().username() ).isEqualTo( "mbladel" );
			assertThat( systemUser.getId().registrationId() ).isNotNull();
		} );
	}

	@Entity(name = "SystemUser")
	public static class SystemUser {
		@EmbeddedId
		private PK id;

		private String name;

		public PK getId() {
			return id;
		}

		public void setId(PK id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public record PK(String username, Integer registrationId) {
	}
}
