/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test checks that when revision number overflow occurs an {@link AuditException} is thrown.
 * <p>
 * In order to test this use case, the {@code REVISION_GENERATOR} is explicitly initialized at
 * {@link Integer.MAX_VALUE} and we attempt to persist two entities that are audited.  The
 * expectation is that the test should persist the first entity but the second should throw the
 * desired exception.
 * <p>
 * Revision numbers should always be positive values and always increasing, this is due to the
 * nature of how the {@link org.hibernate.envers.AuditReader} builds audit queries.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-6615")
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, RevisionNumberOverflowTest.CustomCappedRevEntity.class})
public class RevisionNumberOverflowTest {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Save entity with maximum possible revision number
		scope.inTransaction( entityManager -> {
			final StrTestEntity entity = new StrTestEntity( "test1" );
			entityManager.persist( entity );
		} );

		// Save entity with overflow revision number
		try {
			scope.inTransaction( entityManager -> {
				final StrTestEntity entity = new StrTestEntity( "test2" );
				entityManager.persist( entity );
			} );
		}
		catch (Exception e) {
			assertRootCause( e, AuditException.class, "Negative revision numbers are not allowed" );
		}
	}

	@Test
	public void testRevisionExpectations(EntityManagerFactoryScope scope) {
		final StrTestEntity expected = new StrTestEntity( "test1", 1 );
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// Verify there was only one entity instance saved
			List results = auditReader.createQuery().forRevisionsOfEntity( StrTestEntity.class, true, true )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( expected, results.get( 0 ) );

			// Verify entity instance saved has revision Integer.MAX_VALUE
			assertEquals( expected, auditReader.find( StrTestEntity.class, 1, Integer.MAX_VALUE ) );
		} );
	}

	private static void assertRootCause(Exception exception, Class<?> type, String message) {
		Throwable root = exception;
		while ( root.getCause() != null ) {
			root = root.getCause();
		}
		assertTyping( type, root );
		assertEquals( message, root.getMessage() );
	}

	// We create a custom revision entity here with an explicit configuration for the revision
	// number generation that is explicitly initialized at Integer.MAX_VALUE.  This allows the
	// test to attempt to persist two entities where the first will not trigger a revision
	// number overflow; however the second attempt to persist an entity will.

	@Entity(name = "CustomCappedRevEntity")
	@GenericGenerator(name = "EnversCappedRevisionNumberGenerator",
			strategy = "org.hibernate.id.enhanced.TableGenerator",
			parameters = {
					@Parameter(name = TableGenerator.TABLE_PARAM, value = "REVISION_GENERATOR"),
					@Parameter(name = TableGenerator.INITIAL_PARAM, value = "2147483647"),
					@Parameter(name = TableGenerator.INCREMENT_PARAM, value = "1"),
					@Parameter(name = TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, value = "true")
			})
	@RevisionEntity
	public static class CustomCappedRevEntity {
		@Id
		@GeneratedValue(generator = "EnversCappedRevisionNumberGenerator")
		@RevisionNumber
		private int rev;

		@RevisionTimestamp
		private long timestamp;

		public int getRev() {
			return rev;
		}

		public void setRev(int rev) {
			this.rev = rev;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CustomCappedRevEntity that = (CustomCappedRevEntity) o;
			return rev == that.rev &&
				timestamp == that.timestamp;
		}

		@Override
		public int hashCode() {
			return Objects.hash( rev, timestamp );
		}
	}
}
