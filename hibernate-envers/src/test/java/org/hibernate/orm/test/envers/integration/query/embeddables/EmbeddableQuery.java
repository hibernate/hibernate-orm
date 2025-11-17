/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.embeddables;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Test which supports using {@link AuditEntity} to test equality/inequality
 * between embeddable components.
 *
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {
		Person.class,
		NameInfo.class
})
@EnversTest
@JiraKey(value = "HHH-9178")
public class EmbeddableQuery {
	private Integer personId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		this.personId = scope.fromTransaction( entityManager -> {
			NameInfo ni = new NameInfo( "John", "Doe" );
			Person person1 = new Person( "JDOE", ni );
			entityManager.persist( person1 );
			return person1.getId();
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			Person person1 = entityManager.find( Person.class, personId );
			person1.getNameInfo().setFirstName( "Jane" );
			entityManager.merge( person1 );
		} );

		// Revision 3
		scope.inTransaction( entityManager -> {
			Person person1 = entityManager.find( Person.class, personId );
			person1.setName( "JDOE2" );
			entityManager.merge( person1 );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( 3, AuditReaderFactory.get( em ).getRevisions( Person.class, personId ).size() );
		} );
	}

	@Test
	public void testAuditQueryUsingEmbeddableEquals(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final NameInfo nameInfo = new NameInfo( "John", "Doe" );
			final AuditQuery query = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Person.class, 1 );
			query.add( AuditEntity.property( "nameInfo" ).eq( nameInfo ) );
			List<?> results = query.getResultList();
			assertEquals( 1, results.size() );
			final Person person = (Person) results.get( 0 );
			assertEquals( nameInfo, person.getNameInfo() );
		} );
	}

	@Test
	public void testAuditQueryUsingEmbeddableNotEquals(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final NameInfo nameInfo = new NameInfo( "Jane", "Doe" );
			final AuditQuery query = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Person.class, 1 );
			query.add( AuditEntity.property( "nameInfo" ).ne( nameInfo ) );
			assertEquals( 0, query.getResultList().size() );
		} );
	}

	@Test
	public void testAuditQueryUsingEmbeddableNonEqualityCheck(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			try {
				final NameInfo nameInfo = new NameInfo( "John", "Doe" );
				final AuditQuery query = AuditReaderFactory.get( em ).createQuery()
						.forEntitiesAtRevision( Person.class, 1 );
				query.add( AuditEntity.property( "nameInfo" ).le( nameInfo ) );
			}
			catch (Exception ex) {
				assertInstanceOf( AuditException.class, ex );
			}
		} );
	}
}
