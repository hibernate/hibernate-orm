/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.generated;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.generated.SimpleEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-10841")
@Disabled("NYI - Querying generated column values post insert")
public class GeneratedColumnTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer entityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					SimpleEntity se = new SimpleEntity();
					se.setData( "data" );
					entityManager.persist( se );

					entityId = se.getId();
				},

				// Revision 2
				entityManager -> {
					entityManager.clear();
					SimpleEntity se = entityManager.find( SimpleEntity.class, entityId );
					se.setData( "data2" );
					entityManager.merge( se );
				},

				// Revision 3
				entityManager -> {
					SimpleEntity se = entityManager.find( SimpleEntity.class, entityId );
					entityManager.remove( se );
				}
		);
	}

	@DynamicTest
	public void getRevisionCounts() {
		assertThat( getAuditReader().getRevisions( SimpleEntity.class, entityId ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		// revision - insertion
		final SimpleEntity rev1 = getAuditReader().find( SimpleEntity.class, entityId, 1 );
		assertThat( rev1.getData(), equalTo( "data" ) );
		assertThat( rev1.getCaseNumberInsert(), equalTo( 1 ) );

		// revision - update
		final SimpleEntity rev2 = getAuditReader().find( SimpleEntity.class, entityId, 2 );
		assertThat( rev2.getData(), equalTo( "data2" ) );
		assertThat( rev2.getCaseNumberInsert(), equalTo( 1 ) );

		// revision - deletion
		final SimpleEntity rev3 = getAuditReader().find( SimpleEntity.class, entityId, 3 );
		assertThat( rev3.getData(), nullValue() );
		assertThat( rev3.getCaseNumberInsert(), nullValue() );
	}
}
