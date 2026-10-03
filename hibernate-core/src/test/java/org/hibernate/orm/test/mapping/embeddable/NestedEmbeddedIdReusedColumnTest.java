package org.hibernate.orm.test.mapping.embeddable;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies that a read-only attribute may reuse a column inside a nested embedded identifier.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		NestedEmbeddedIdReusedColumnTest.Participant.class,
		NestedEmbeddedIdReusedColumnTest.ParticipantId.class,
		NestedEmbeddedIdReusedColumnTest.ParticipantRegistration.class
})
@SessionFactory
@JiraKey("HHH-16906")
public class NestedEmbeddedIdReusedColumnTest {
	@Test
	void testReadOnlyDuplicateColumn(SessionFactoryScope scope) {
		// Bootstrapping the SessionFactory must accept the duplicate read-only column.
		scope.inTransaction( session -> {
			final Participant participant = new Participant();
			participant.id = new ParticipantId();
			participant.id.registrationNumber = new ParticipantRegistration();
			participant.id.registrationNumber.registrationIndex = "registration-1";
			participant.id.registrationNumber.registrationDisplay = "registration-1";
			session.persist( participant );
		} );

		scope.inTransaction( session -> {
			final Participant participant = session.createQuery( "from Participant", Participant.class )
					.getSingleResult();
			assertEquals( "registration-1", participant.id.registrationNumber.registrationIndex );
			assertEquals( "registration-1", participant.id.registrationNumber.registrationDisplay );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Participant")
	@Table(name = "hhh16906_participant")
	public static class Participant {
		@EmbeddedId
		ParticipantId id;
	}

	@Embeddable
	public static class ParticipantId implements Serializable {
		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "registrationIndex", column = @Column(name = "REGISTRATION_INDEX")),
				@AttributeOverride(name = "registrationDisplay", column = @Column(
						name = "REGISTRATION_INDEX", insertable = false, updatable = false
				))
		})
		ParticipantRegistration registrationNumber;

		@Override
		public boolean equals(Object object) {
			return object instanceof ParticipantId that
					&& Objects.equals( registrationNumber, that.registrationNumber );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( registrationNumber );
		}
	}

	@Embeddable
	public static class ParticipantRegistration implements Serializable {
		String registrationIndex;
		String registrationDisplay;

		@Override
		public boolean equals(Object object) {
			return object instanceof ParticipantRegistration that
					&& Objects.equals( registrationIndex, that.registrationIndex )
					&& Objects.equals( registrationDisplay, that.registrationDisplay );
		}

		@Override
		public int hashCode() {
			return Objects.hash( registrationIndex, registrationDisplay );
		}
	}
}
