package org.hibernate.orm.test.jpa.query;

import java.util.List;
import java.util.UUID;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// An inverse one-to-one entity parameter must not reuse the following parameter's JDBC position.
///
/// @author Steve Ebersole
@JiraKey( "HHH-14716" )
@Jpa( annotatedClasses = {
		InverseOneToOnePositionalParameterTest.DbUser.class,
		InverseOneToOnePositionalParameterTest.ContactInfo.class,
		InverseOneToOnePositionalParameterTest.Subject.class,
		InverseOneToOnePositionalParameterTest.DbScreening.class
} )
public class InverseOneToOnePositionalParameterTest {
	private static final UUID FIRST_USER = UUID.fromString( "00000000-0000-0000-0000-000000000001" );
	private static final UUID SECOND_USER = UUID.fromString( "00000000-0000-0000-0000-000000000002" );

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ContactInfo firstContact = new ContactInfo();
			firstContact.id = 1L;
			entityManager.persist( firstContact );
			final ContactInfo secondContact = new ContactInfo();
			secondContact.id = 2L;
			entityManager.persist( secondContact );
			final DbUser first = new DbUser();
			first.id = FIRST_USER;
			first.email = "first@example.org";
			first.contactInfo = firstContact;
			firstContact.user = first;
			entityManager.persist( first );
			final DbUser second = new DbUser();
			second.id = SECOND_USER;
			second.email = "second@example.org";
			second.contactInfo = secondContact;
			secondContact.user = second;
			entityManager.persist( second );
			final Subject firstSubject = new Subject();
			firstSubject.id = 1L;
			firstSubject.contactInfo = firstContact;
			entityManager.persist( firstSubject );
			final Subject secondSubject = new Subject();
			secondSubject.id = 2L;
			secondSubject.contactInfo = secondContact;
			entityManager.persist( secondSubject );
			persistScreening( entityManager, 1L, firstSubject, second );
			persistScreening( entityManager, 2L, firstSubject, first );
			persistScreening( entityManager, 3L, secondSubject, second );
		} );
	}

	private static void persistScreening(jakarta.persistence.EntityManager entityManager, long id, Subject subject, DbUser referent) {
		final DbScreening screening = new DbScreening();
		screening.id = id;
		screening.subject = subject;
		screening.referent = referent;
		entityManager.persist( screening );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testInverseAssociationAndStringParameters(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final DbUser subject = entityManager.getReference( DbUser.class, FIRST_USER );
			final var query = entityManager.createQuery(
					"select distinct u from DbScreening s inner join s.referent u "
							+ "where s.subject.contactInfo.user = ?1 and upper(u.email) = upper(?2)", DbUser.class )
					.setParameter( 1, subject )
					.setParameter( 2, "SECOND@EXAMPLE.ORG" );
			final List<DbUser> result = query.getResultList();
			assertEquals( List.of( SECOND_USER ), result.stream().map( user -> user.id ).toList() );
			assertEquals( "second@example.org", result.get( 0 ).email );
			query.setParameter( 1, entityManager.getReference( DbUser.class, SECOND_USER ) );
			query.setParameter( 2, "FIRST@EXAMPLE.ORG" );
			assertEquals( List.of(), query.getResultList() );
		} );
	}

	@Entity( name = "DbUser" )
	@Table( name = "hhh14716_user" )
	public static class DbUser {
		@Id
		private UUID id;
		private String email;
		@OneToOne( fetch = FetchType.LAZY )
		private ContactInfo contactInfo;
	}

	@Entity( name = "ContactInfo" )
	@Table( name = "hhh14716_contact" )
	public static class ContactInfo {
		@Id
		private Long id;
		@OneToOne( mappedBy = "contactInfo", fetch = FetchType.LAZY )
		private DbUser user;
	}

	@Entity( name = "Subject" )
	@Table( name = "hhh14716_subject" )
	public static class Subject {
		@Id
		private Long id;
		@OneToOne( fetch = FetchType.LAZY )
		private ContactInfo contactInfo;
	}

	@Entity( name = "DbScreening" )
	@Table( name = "hhh14716_screening" )
	public static class DbScreening {
		@Id
		private Long id;
		@ManyToOne( fetch = FetchType.LAZY )
		private Subject subject;
		@ManyToOne( fetch = FetchType.LAZY )
		private DbUser referent;
	}
}
