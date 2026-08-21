/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jira("https://hibernate.atlassian.net/browse/HHH-20357")
@EnversTest
@Jpa(annotatedClasses = {
		JoinedInheritanceAuditTableReadOrderingTest.PartyEntity.class,
		JoinedInheritanceAuditTableReadOrderingTest.PersonEntity.class,
		JoinedInheritanceAuditTableReadOrderingTest.PartyAuditView.class,
		JoinedInheritanceAuditTableReadOrderingTest.PersonAuditView.class
})
public class JoinedInheritanceAuditTableReadOrderingTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		Long id = scope.fromTransaction( em -> {
			PersonEntity person = new PersonEntity();
			person.setId( 1L );
			person.setFullName( "fullName.1" );
			person.setFirstName( "firstName.1" );
			person.setLastName( "lastName.1" );
			em.persist( person );

			return person.getId();
		} );

		scope.inTransaction( em -> {
			PersonEntity person = em.find( PersonEntity.class, id );
			person.setFullName( "fullName.2" );
			person.setFirstName( "firstName.2" );
			person.setLastName( "lastName.2" );
		} );

		scope.inTransaction( em -> {
			PersonEntity person = em.find( PersonEntity.class, id );
			person.setFullName( "fullName.3" );
			person.setFirstName( "firstName.3" );
			person.setLastName( "lastName.3" );
		} );
	}

	@Test
	public void testAuditRowsExist(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( PersonEntity.class, 1L ) );
		} );
	}

	@Test
	public void testJoinedAuditTableReadReturnsAllRowsEnvers(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			PersonEntity person1 = AuditReaderFactory.get( em ).find( PersonEntity.class, 1L, 1 );
			PersonEntity person2 = AuditReaderFactory.get( em ).find( PersonEntity.class, 1L, 2 );
			PersonEntity person3 = AuditReaderFactory.get( em ).find( PersonEntity.class, 1L, 3 );

			assertEquals( "firstName.1", person1.getFirstName() );
			assertEquals( "firstName.2", person2.getFirstName() );
			assertEquals( "firstName.3", person3.getFirstName() );
		} );
	}

	@Test
	public void testJoinedAuditTableReadReturnsAllRowsCustom(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<PersonAuditView> audits = em.createQuery(
					"select p from PersonAuditView p order by p.revisionId",
					PersonAuditView.class
			).getResultList();

			assertEquals( 3, audits.size() );
			assertEquals( "firstName.1", audits.get( 0 ).getFirstName() );
			assertEquals( "firstName.2", audits.get( 1 ).getFirstName() );
			assertEquals( "firstName.3", audits.get( 2 ).getFirstName() );
		} );
	}


	public static class AuditId implements Serializable {
		private Long entityId;
		private Integer revisionId;

		public AuditId() {
		}

		public AuditId(Long entityId, Integer revisionId) {
			this.entityId = entityId;
			this.revisionId = revisionId;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof AuditId ) ) {
				return false;
			}
			AuditId auditId = (AuditId) o;
			return Objects.equals( entityId, auditId.entityId ) && Objects.equals( revisionId, auditId.revisionId );
		}

		@Override
		public int hashCode() {
			return Objects.hash( entityId, revisionId );
		}
	}

	@Entity(name = "PartyEntity")
	@Table(name = "PARTY")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class PartyEntity {
		@Id
		@Column(name = "PTY_ID")
		private Long id;

		@Column(name = "FULL_NAME")
		private String fullName;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}
	}

	@Entity(name = "PersonEntity")
	@Table(name = "PERSON")
	@Audited
	public static class PersonEntity extends PartyEntity {
		@Column(name = "FIRST_NAME")
		private String firstName;

		@Column(name = "LAST_NAME")
		private String lastName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}

	@MappedSuperclass
	@IdClass(AuditId.class)
	public abstract static class BaseAuditView {
		@Id
		@Column(name = "PTY_ID")
		private Long entityId;

		@Id
		@Column(name = "REV")
		private Integer revisionId;

		public Long getEntityId() {
			return entityId;
		}

		public Integer getRevisionId() {
			return revisionId;
		}
	}

	@Entity(name = "PartyAuditView")
	@Table(name = "PARTY_AUD")
	@Immutable
	@Inheritance(strategy = InheritanceType.JOINED)
	@AttributeOverride(name = "entityId", column = @Column(name = "PTY_ID"))
	public abstract static class PartyAuditView extends BaseAuditView {
		@Column(name = "FULL_NAME")
		private String fullName;

		public String getFullName() {
			return fullName;
		}
	}

	@Entity(name = "PersonAuditView")
	@Table(name = "PERSON_AUD")
	public static class PersonAuditView extends PartyAuditView {
		@Column(name = "FIRST_NAME")
		private String firstName;

		@Column(name = "LAST_NAME")
		private String lastName;

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}
	}
}
