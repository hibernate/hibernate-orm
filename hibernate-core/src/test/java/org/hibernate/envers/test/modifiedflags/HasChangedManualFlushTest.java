/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7918")
public class HasChangedManualFlushTest extends AbstractModifiedFlagsEntityTest {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BasicAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					BasicAuditedEntity entity = new BasicAuditedEntity( "str1", 1 );
					entityManager.persist( entity );
					this.id = entity.getId();
				},
				// Revision 2 - both properties (str1 and long1) should be marked as modified
				entityManager -> {
					BasicAuditedEntity entity = entityManager.find( BasicAuditedEntity.class, id );

					entity.setStr1( "str2" );
					entity = entityManager.merge( entity );
					entityManager.flush();

					entity.setLong1( 2 );
					entity = entityManager.merge( entity );
					entityManager.flush();
				}
		);
	}

	@DynamicTest
	public void testHasChangedOnDoubleFlush() {
		final List str1Changes = queryForPropertyHasChanged( BasicAuditedEntity.class, id, "str1" );
		assertThat( extractRevisions( str1Changes ), contains( 1, 2 ) );

		final List long1Changes = queryForPropertyHasChanged( BasicAuditedEntity.class, id, "long1" );
		assertThat( extractRevisions( long1Changes ), contains( 1, 2 ) );
	}
}
