/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.basic.BasicPartialAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedUnversionedPropertiesTest extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BasicPartialAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final BasicPartialAuditedEntity entity = new BasicPartialAuditedEntity( "x", "a" );
					entityManager.persist( entity );
					this.id1 = entity.getId();
				},

				// Should not generate a revision
				entityManager -> {
					final BasicPartialAuditedEntity entity = entityManager.find( BasicPartialAuditedEntity.class, id1 );
					entity.setStr1( "x" );
					entity.setStr2( "a" );
				},

				// Revision 2
				entityManager -> {
					final BasicPartialAuditedEntity entity = entityManager.find( BasicPartialAuditedEntity.class, id1 );
					entity.setStr1( "y" );
					entity.setStr2( "b" );
				},

				// Should not generate a revision
				entityManager -> {
					final BasicPartialAuditedEntity entity = entityManager.find( BasicPartialAuditedEntity.class, id1 );
					entity.setStr1( "y" );
					entity.setStr2( "c" );
				}
		);
	}

	@DynamicTest
	public void testHasChangedQuery() throws Exception {
		final List str1Changes1 = queryForPropertyHasChanged( BasicPartialAuditedEntity.class, id1, "str1" );
		assertThat( extractRevisions( str1Changes1 ), contains( 1, 2 ) );
	}

	@DynamicTest(expected = IllegalArgumentException.class)
	public void testExceptionOnHasChangedQuery() throws Exception {
		queryForPropertyHasChangedWithDeleted( BasicPartialAuditedEntity.class, id1, "str2" );
	}
}
