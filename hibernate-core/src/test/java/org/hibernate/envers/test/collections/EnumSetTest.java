/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.EnumSetEntity;
import org.hibernate.envers.test.support.domains.collections.EnumSetEntity.E1;
import org.hibernate.envers.test.support.domains.collections.EnumSetEntity.E2;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
@Disabled("Specifying explicit-type as enum in HBM no longer works, e.g. type='blah.blah.MyEnum'")
public class EnumSetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer sse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EnumSetEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (sse1: initially 1 element)
				entityManager -> {
					EnumSetEntity sse1 = new EnumSetEntity();

					sse1.getEnums1().add( E1.X );
					sse1.getEnums2().add( E2.A );

					entityManager.persist( sse1 );

					this.sse1_id = sse1.getId();
				},

				// Revision 2 (sse1: adding 1 element/removing a non-existing element)
				entityManager -> {
					EnumSetEntity sse1 = entityManager.find( EnumSetEntity.class, sse1_id );

					sse1.getEnums1().add( E1.Y );
					sse1.getEnums2().remove( E2.B );
				},

				// Revision 3 (sse1: removing 1 element/adding an exisiting element)
				entityManager -> {
					EnumSetEntity sse1 = entityManager.find( EnumSetEntity.class, sse1_id );

					sse1.getEnums1().remove( E1.X );
					sse1.getEnums2().add( E2.A );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( EnumSetEntity.class, sse1_id ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfSse1() {
		EnumSetEntity rev1 = getAuditReader().find( EnumSetEntity.class, sse1_id, 1 );
		EnumSetEntity rev2 = getAuditReader().find( EnumSetEntity.class, sse1_id, 2 );
		EnumSetEntity rev3 = getAuditReader().find( EnumSetEntity.class, sse1_id, 3 );

		assertThat( rev1.getEnums1(), contains( E1.X ) );
		assertThat( rev1.getEnums2(), contains( E2.A ) );

		assertThat( rev2.getEnums1(), contains( E1.X, E1.Y ) );
		assertThat( rev2.getEnums2(), contains( E2.A ) );

		assertThat( rev3.getEnums1(), contains( E1.Y ) );
		assertThat( rev3.getEnums2(), contains( E2.A ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7780")
	public void testEnumRepresentation() {
		final String ENUM1_QUERY = "SELECT enums1 FROM EnumSetEntity_enums1_AUD ORDER BY REV ASC";
		final String ENUM2_QUERY = "SELECT enums2 FROM EnumSetEntity_enums2_AUD ORDER BY REV ASC";

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					List<String> enums1 = entityManager.createNativeQuery( ENUM1_QUERY, String.class ).getResultList();
					List<Number> enums2 = entityManager.createNativeQuery( ENUM2_QUERY, Number.class ).getResultList();

					assertThat( enums1, contains( "X", "Y", "X" ) );

					assertThat( enums2, CollectionMatchers.hasSize( 1 ) );

					// Compare as Strings to account for Oracle returning a BigDecimal instead of int.
					assertThat( enums2.get( 0 ).toString(), is( "0" ) );
				}
		);
	}
}