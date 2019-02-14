/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.StringSetEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class StringSetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer sse1_id;
	private Integer sse2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StringSetEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (sse1: initialy empty, sse2: initialy 2 elements)
				entityManager -> {
					StringSetEntity sse1 = new StringSetEntity();
					StringSetEntity sse2 = new StringSetEntity();

					sse2.getStrings().add( "sse2_string1" );
					sse2.getStrings().add( "sse2_string2" );

					entityManager.persist( sse1 );
					entityManager.persist( sse2 );

					this.sse1_id = sse1.getId();
					this.sse2_id = sse2.getId();
				},

				// Revision 2 (sse1: adding 2 elements, sse2: adding an existing element)
				entityManager -> {
					StringSetEntity sse1 = entityManager.find( StringSetEntity.class, sse1_id );
					StringSetEntity sse2 = entityManager.find( StringSetEntity.class, sse2_id );

					sse1.getStrings().add( "sse1_string1" );
					sse1.getStrings().add( "sse1_string2" );

					sse2.getStrings().add( "sse2_string1" );
				},

				// Revision 3 (sse1: removing a non-existing element, sse2: removing one element)
				entityManager -> {
					StringSetEntity sse1 = entityManager.find( StringSetEntity.class, sse1_id );
					StringSetEntity sse2 = entityManager.find( StringSetEntity.class, sse2_id );

					sse1.getStrings().remove( "sse1_string3" );
					sse2.getStrings().remove( "sse2_string1" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StringSetEntity.class, sse1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( StringSetEntity.class, sse2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfSse1() {
		StringSetEntity rev1 = getAuditReader().find( StringSetEntity.class, sse1_id, 1 );
		StringSetEntity rev2 = getAuditReader().find( StringSetEntity.class, sse1_id, 2 );
		StringSetEntity rev3 = getAuditReader().find( StringSetEntity.class, sse1_id, 3 );

		assertThat( rev1.getStrings(), empty() );
		assertThat( rev2.getStrings(), containsInAnyOrder( "sse1_string1", "sse1_string2" ) );
		assertThat( rev3.getStrings(), containsInAnyOrder( "sse1_string1", "sse1_string2" ) );
	}

	@DynamicTest
	public void testHistoryOfSse2() {
		StringSetEntity rev1 = getAuditReader().find( StringSetEntity.class, sse2_id, 1 );
		StringSetEntity rev2 = getAuditReader().find( StringSetEntity.class, sse2_id, 2 );
		StringSetEntity rev3 = getAuditReader().find( StringSetEntity.class, sse2_id, 3 );

		assertThat( rev1.getStrings(), containsInAnyOrder( "sse2_string1", "sse2_string2" ) );
		assertThat( rev2.getStrings(), containsInAnyOrder( "sse2_string1", "sse2_string2" ) );
		assertThat( rev3.getStrings(), containsInAnyOrder( "sse2_string2" ) );
	}
}