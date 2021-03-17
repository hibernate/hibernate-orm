/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.id.enhanced.TableGenerator;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * This test checks that when revision number overflow occurs an {@link AuditException} is thrown.
 *
 * In order to test this use case, the {@code REVISION_GENERATOR} is explicitly initialized at
 * {@link Integer.MAX_VALUE} and we attempt to persist two entities that are audited.  The
 * expectation is that the test should persist the first entity but the second should throw the
 * desired exception.
 *
 * Revision numbers should always be positive values and always increasing, this is due to the
 * nature of how the {@link org.hibernate.envers.AuditReader} builds audit queries.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-6615")
public class RevisionNumberOverflowTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class, CustomCappedRevEntity.class };
	}

	@Priority(10)
	@Test
	public void initData() {
		// Save entity with maximum possible revision number
		doInJPA( this::entityManagerFactory, entityManager -> {
			final StrTestEntity entity = new StrTestEntity( "test1" );
			entityManager.persist( entity );
		} );

		// Save entity with overflow revision number
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				final StrTestEntity entity = new StrTestEntity( "test2" );
				entityManager.persist( entity );
			} );
		} catch ( Exception e ) {
			assertRootCause( e, AuditException.class, "Negative revision numbers are not allowed" );
		}
	}

	@Test
	public void testRevisionExpectations() {
		final StrTestEntity expected = new StrTestEntity( "test1", 1 );

		// Verify there was only one entity instance saved
		List results = getAuditReader().createQuery().forRevisionsOfEntity( StrTestEntity.class, true, true ).getResultList();
		assertEquals( 1, results.size() );
		assertEquals( expected, results.get( 0 ) );

		// Verify entity instance saved has revision Integer.MAX_VALUE
		assertEquals( expected, getAuditReader().find( StrTestEntity.class, 1, Integer.MAX_VALUE ) );
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
