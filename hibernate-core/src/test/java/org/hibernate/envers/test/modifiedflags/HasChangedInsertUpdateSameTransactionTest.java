/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11582")
public class HasChangedInsertUpdateSameTransactionTest extends AbstractModifiedFlagsEntityTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final BasicAuditedEntity entity = new BasicAuditedEntity( "str1", 1L );
					entityManager.persist( entity );
					entity.setStr1( "str2" );
					entityManager.merge( entity );
				}
		);
	}

	@DynamicTest
	public void testPropertyChangedInsrtUpdateSameTransaction() {
		// this was only flagged as changed as part of the persist
		assertThat( queryForPropertyHasChanged( BasicAuditedEntity.class, 1, "long1" ), CollectionMatchers.hasSize( 1 ) );
	}
}
