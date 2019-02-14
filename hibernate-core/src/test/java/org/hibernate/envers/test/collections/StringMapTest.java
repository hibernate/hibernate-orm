/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import org.hamcrest.Matchers;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.StringMapEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class StringMapTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer sme1_id;
	private Integer sme2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StringMapEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (sme1: initially empty, sme2: initially 1 mapping)
				entityManager -> {
					StringMapEntity sme1 = new StringMapEntity();
					StringMapEntity sme2 = new StringMapEntity();

					sme2.getStrings().put( "1", "a" );

					entityManager.persist( sme1 );
					entityManager.persist( sme2 );

					this.sme1_id = sme1.getId();
					this.sme2_id = sme2.getId();
				},

				// Revision 2 (sme1: adding 2 mappings, sme2: no changes)
				entityManager -> {
					StringMapEntity sme1 = entityManager.find( StringMapEntity.class, sme1_id );
					StringMapEntity sme2 = entityManager.find( StringMapEntity.class, sme2_id );

					sme1.getStrings().put( "1", "a" );
					sme1.getStrings().put( "2", "b" );
				},

				// Revision 3 (sme1: removing an existing mapping, sme2: replacing a value)
				entityManager -> {
					StringMapEntity sme1 = entityManager.find( StringMapEntity.class, sme1_id );
					StringMapEntity sme2 = entityManager.find( StringMapEntity.class, sme2_id );

					sme1.getStrings().remove( "1" );
					sme2.getStrings().put( "1", "b" );
				},

				// No revision (sme1: removing a non-existing mapping, sme2: replacing with the same value)
				entityManager -> {
					StringMapEntity sme1 = entityManager.find( StringMapEntity.class, sme1_id );
					StringMapEntity sme2 = entityManager.find( StringMapEntity.class, sme2_id );

					sme1.getStrings().remove( "3" );
					sme2.getStrings().put( "1", "b" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StringMapEntity.class, sme1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( StringMapEntity.class, sme2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfSse1() {
		StringMapEntity rev1 = getAuditReader().find( StringMapEntity.class, sme1_id, 1 );
		StringMapEntity rev2 = getAuditReader().find( StringMapEntity.class, sme1_id, 2 );
		StringMapEntity rev3 = getAuditReader().find( StringMapEntity.class, sme1_id, 3 );
		StringMapEntity rev4 = getAuditReader().find( StringMapEntity.class, sme1_id, 4 );

		assertThat( rev1.getStrings().entrySet(), Matchers.empty() );
		assertThat( rev2.getStrings().keySet(), hasSize( 2 ) );
		assertThat( rev2.getStrings(), hasEntry( "1", "a" ) );
		assertThat( rev2.getStrings(), hasEntry( "2", "b" ) );
		assertThat( rev3.getStrings().keySet(), hasSize( 1 ) );
		assertThat( rev3.getStrings(), hasEntry( "2", "b" ) );
		assertThat( rev4.getStrings().keySet(), hasSize( 1 ) );
		assertThat( rev4.getStrings(), hasEntry( "2", "b" ) );
	}

	@DynamicTest
	public void testHistoryOfSse2() {
		StringMapEntity rev1 = getAuditReader().find( StringMapEntity.class, sme2_id, 1 );
		StringMapEntity rev2 = getAuditReader().find( StringMapEntity.class, sme2_id, 2 );
		StringMapEntity rev3 = getAuditReader().find( StringMapEntity.class, sme2_id, 3 );
		StringMapEntity rev4 = getAuditReader().find( StringMapEntity.class, sme2_id, 4 );

		assertThat( rev1.getStrings().keySet(), hasSize( 1 ) );
		assertThat( rev1.getStrings(), hasEntry( "1", "a" ) );
		assertThat( rev2.getStrings().keySet(), hasSize( 1 ) );
		assertThat( rev2.getStrings(), hasEntry( "1", "a" ) );
		assertThat( rev3.getStrings().keySet(), hasSize( 1 ) );
		assertThat( rev3.getStrings(), hasEntry( "1", "b" ) );
		assertThat( rev4.getStrings().keySet(), hasSize( 1 ) );
		assertThat( rev4.getStrings(), hasEntry( "1", "b" ) );
	}
}