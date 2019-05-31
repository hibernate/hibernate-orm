/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.EnumMapEntity;
import org.hibernate.envers.test.support.domains.collections.EnumMapEntity.EnumType;
import org.hibernate.envers.test.support.domains.collections.EnumMapType;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-6374")
@Disabled("Attempts to instantiate EnumType enum via ManagedBeanRegistry throwing unable to locate no-arg constructor for bean.")
public class EnumMapTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer entityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EnumMapEntity.class, EnumMapType.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					EnumMapEntity entity = new EnumMapEntity();
					entity.getTypes().put( EnumType.TYPE_A, new EnumMapType( "A" ) );
					entity.getTypes().put( EnumType.TYPE_B, new EnumMapType( "B" ) );
					entityManager.persist( entity );

					entityId = entity.getId();
				},

				// Revision 2
				entityManager -> {
					final EnumMapEntity entity = entityManager.find( EnumMapEntity.class, entityId );
					entity.getTypes().remove( EnumType.TYPE_A );
					entity.getTypes().put( EnumType.TYPE_C, new EnumMapType( "C" ) );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCount() {
		assertThat( getAuditReader().getRevisions( EnumMapEntity.class, entityId ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testAuditEnumMapCollection() {
		EnumMapEntity rev1 = getAuditReader().find( EnumMapEntity.class, entityId, 1 );
		assertThat( rev1.getTypes().keySet(), containsInAnyOrder( EnumType.TYPE_A, EnumType.TYPE_B ) );

		EnumMapEntity rev2 = getAuditReader().find( EnumMapEntity.class, entityId, 2 );
		assertThat( rev2.getTypes().keySet(), containsInAnyOrder( EnumType.TYPE_B, EnumType.TYPE_C ) );
	}

}
