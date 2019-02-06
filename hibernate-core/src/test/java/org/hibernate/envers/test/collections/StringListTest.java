/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.StringListEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class StringListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer sle1_id;
	private Integer sle2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StringListEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		entityManagerFactoryScope().inTransactions(
				// Revision 1 (sle1: initially empty, sle2: initially 2 elements)
				entityManager -> {
					StringListEntity sle1 = new StringListEntity();
					StringListEntity sle2 = new StringListEntity();

					sle2.getStrings().add( "sle2_string1" );
					sle2.getStrings().add( "sle2_string2" );

					entityManager.persist( sle1 );
					entityManager.persist( sle2 );

					this.sle1_id = sle1.getId();
					this.sle2_id = sle2.getId();
				},

				// Revision 2 (sle1: adding 2 elements, sle2: adding an existing element)
				entityManager -> {
					StringListEntity sle1 = entityManager.find( StringListEntity.class, sle1_id );
					StringListEntity sle2 = entityManager.find( StringListEntity.class, sle2_id );

					sle1.getStrings().add( "sle1_string1" );
					sle1.getStrings().add( "sle1_string2" );

					sle2.getStrings().add( "sle2_string1" );
				},

				// Revision 3 (sle1: replacing an element at index 0, sle2: removing an element at index 0)
				entityManager -> {
					StringListEntity sle1 = entityManager.find( StringListEntity.class, sle1_id );
					StringListEntity sle2 = entityManager.find( StringListEntity.class, sle2_id );

					sle1.getStrings().set( 0, "sle1_string3" );

					sle2.getStrings().remove( 0 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StringListEntity.class, sle1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( StringListEntity.class, sle2_id ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfSle1() {
		StringListEntity rev1 = getAuditReader().find( StringListEntity.class, sle1_id, 1 );
		StringListEntity rev2 = getAuditReader().find( StringListEntity.class, sle1_id, 2 );
		StringListEntity rev3 = getAuditReader().find( StringListEntity.class, sle1_id, 3 );

		assertThat( rev1.getStrings(), empty() );
		assertThat( rev2.getStrings(), contains( "sle1_string1", "sle1_string2" ) );
		assertThat( rev3.getStrings(), contains( "sle1_string3", "sle1_string2" ) );
	}

	@DynamicTest
	public void testHistoryOfSse2() {
		StringListEntity rev1 = getAuditReader().find( StringListEntity.class, sle2_id, 1 );
		StringListEntity rev2 = getAuditReader().find( StringListEntity.class, sle2_id, 2 );
		StringListEntity rev3 = getAuditReader().find( StringListEntity.class, sle2_id, 3 );

		assertThat( rev1.getStrings(), contains( "sle2_string1", "sle2_string2" ) );
		assertThat( rev2.getStrings(), contains( "sle2_string1", "sle2_string2", "sle2_string1" ) );
		assertThat( rev3.getStrings(), contains( "sle2_string2", "sle2_string1" ) );
	}
}