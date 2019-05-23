/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.DefaultValueComponent1;
import org.hibernate.envers.test.support.domains.components.DefaultValueComponent2;
import org.hibernate.envers.test.support.domains.components.DefaultValueComponentTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test class for components with default values.
 *
 * @author Erik-Berndt Scheper
 * @see <a
 *      href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-5288">
 *      Hibernate JIRA </a>
 */
public class DefaultValueComponentsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id0;
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;
	private Integer id5;
	private Integer id6;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { DefaultValueComponentTestEntity.class};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					DefaultValueComponentTestEntity cte0 = DefaultValueComponentTestEntity.of( null );

					DefaultValueComponentTestEntity cte1 = DefaultValueComponentTestEntity.of(
							DefaultValueComponent1.of( "c1-str1", null )
					);

					DefaultValueComponentTestEntity cte2 = DefaultValueComponentTestEntity.of(
							DefaultValueComponent1.of( "c1-str1", DefaultValueComponent2.of( "c2-str1", "c2-str2" ) )
					);

					DefaultValueComponentTestEntity cte3 = DefaultValueComponentTestEntity.of(
							DefaultValueComponent1.of( null, DefaultValueComponent2.of( "c2-str1", "c2-str2" ) )
					);

					DefaultValueComponentTestEntity cte4 = DefaultValueComponentTestEntity.of(
							DefaultValueComponent1.of( null, DefaultValueComponent2.of( null, "c2-str2" ) )
					);

					DefaultValueComponentTestEntity cte5 = DefaultValueComponentTestEntity.of(
							DefaultValueComponent1.of( null, DefaultValueComponent2.of( "c2-str1", null ) )
					);

					DefaultValueComponentTestEntity cte6 = DefaultValueComponentTestEntity.of(
							DefaultValueComponent1.of( null, DefaultValueComponent2.of( null, null ) )
					);
					
					entityManager.persist( cte0 );
					entityManager.persist( cte1 );
					entityManager.persist( cte2 );
					entityManager.persist( cte3 );
					entityManager.persist( cte4 );
					entityManager.persist( cte5 );
					entityManager.persist( cte6 );

					id0 = cte0.getId();
					id1 = cte1.getId();
					id2 = cte2.getId();
					id3 = cte3.getId();
					id4 = cte4.getId();
					id5 = cte5.getId();
					id6 = cte6.getId();
				},

				// Revision 2
				entityManager -> {
					DefaultValueComponentTestEntity cte0 = entityManager.find( DefaultValueComponentTestEntity.class, id0 );
					DefaultValueComponentTestEntity cte1 = entityManager.find( DefaultValueComponentTestEntity.class, id1 );
					DefaultValueComponentTestEntity cte2 = entityManager.find( DefaultValueComponentTestEntity.class, id2 );
					DefaultValueComponentTestEntity cte3 = entityManager.find( DefaultValueComponentTestEntity.class, id3 );
					DefaultValueComponentTestEntity cte4 = entityManager.find( DefaultValueComponentTestEntity.class, id4 );
					DefaultValueComponentTestEntity cte5 = entityManager.find( DefaultValueComponentTestEntity.class, id5 );
					DefaultValueComponentTestEntity cte6 = entityManager.find( DefaultValueComponentTestEntity.class, id6 );

					cte0.setComp1( DefaultValueComponent1.of( "upd-c1-str1", null ) );
					cte1.setComp1( DefaultValueComponent1.of( null, DefaultValueComponent2.of( "upd-c2-str1", "upd-c2-str2" ) ) );

					cte2.getComp1().getComp2().setStr1( "upd-c2-str1" );
					cte3.getComp1().getComp2().setStr1( "upd-c2-str1" );
					cte4.getComp1().getComp2().setStr1( "upd-c2-str1" );
					cte5.getComp1().getComp2().setStr1( "upd-c2-str1" );
					cte6.getComp1().getComp2().setStr1( "upd-c2-str1" );

				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( DefaultValueComponentTestEntity.class, id0 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( DefaultValueComponentTestEntity.class, id1 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( DefaultValueComponentTestEntity.class, id2 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( DefaultValueComponentTestEntity.class, id3 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( DefaultValueComponentTestEntity.class, id4 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( DefaultValueComponentTestEntity.class, id5 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( DefaultValueComponentTestEntity.class, id6 ), contains( 1, 2 ) );
	}

	@DynamicTest
	@Disabled("NYI - Native Query Support")
	public void testHistoryOfId0() {
		final DefaultValueComponentTestEntity ver1 = DefaultValueComponentTestEntity.of(
				id0,
				DefaultValueComponent1.of( null, null )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id0, 1 ), equalTo( ver1 ) );

		final DefaultValueComponentTestEntity ver2 = DefaultValueComponentTestEntity.of(
				id0,
				DefaultValueComponent1.of( "upd-c1-str1", null )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id0, 2 ), equalTo( ver2 ) );

		checkCorrectlyPersisted( id0, null, null );
	}

	@DynamicTest
	@Disabled("NYI - Native Query Support")
	public void testHistoryOfId1() {
		final DefaultValueComponentTestEntity ver1 = DefaultValueComponentTestEntity.of(
				id1,
				DefaultValueComponent1.of( "c1-str1", null )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id1, 1 ), equalTo( ver1 ) );

		final DefaultValueComponentTestEntity ver2 = DefaultValueComponentTestEntity.of(
				id1,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( "upd-c2-str1", "upd-c2-str2" ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id1, 2 ), equalTo( ver2 ) );

		checkCorrectlyPersisted( id1, null, "upd-c2-str1" );
	}

	@DynamicTest
	public void testHistoryOfId2() {
		final DefaultValueComponentTestEntity ver1 = DefaultValueComponentTestEntity.of(
				id2,
				DefaultValueComponent1.of( "c1-str1", DefaultValueComponent2.of( "c2-str1", "c2-str2" ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id2, 1 ), equalTo( ver1 ) );

		final DefaultValueComponentTestEntity ver2 = DefaultValueComponentTestEntity.of(
				id2,
				DefaultValueComponent1.of( "c1-str1", DefaultValueComponent2.of( "upd-c2-str1", "c2-str2" ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id2, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId3() {
		final DefaultValueComponentTestEntity ver1 = DefaultValueComponentTestEntity.of(
				id3,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( "c2-str1", "c2-str2" ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id3, 1 ), equalTo( ver1 ) );

		final DefaultValueComponentTestEntity ver2 = DefaultValueComponentTestEntity.of(
				id3,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( "upd-c2-str1", "c2-str2" ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id3, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId4() {
		final DefaultValueComponentTestEntity ver1 = DefaultValueComponentTestEntity.of(
				id4,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( null, "c2-str2" ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id4, 1 ), equalTo( ver1 ) );

		final DefaultValueComponentTestEntity ver2 = DefaultValueComponentTestEntity.of(
				id4,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( "upd-c2-str1", "c2-str2" ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id4, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId5() {
		final DefaultValueComponentTestEntity ver1 = DefaultValueComponentTestEntity.of(
				id5,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( "c2-str1", null ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id5, 1 ), equalTo( ver1 ) );

		final DefaultValueComponentTestEntity ver2 = DefaultValueComponentTestEntity.of(
				id5,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( "upd-c2-str1", null ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id5, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId6() {
		final DefaultValueComponentTestEntity ver1 = DefaultValueComponentTestEntity.of(
				id6,
				DefaultValueComponent1.of( null, null )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id6, 1 ), equalTo( ver1 ) );

		final DefaultValueComponentTestEntity ver2 = DefaultValueComponentTestEntity.of(
				id6,
				DefaultValueComponent1.of( null, DefaultValueComponent2.of( "upd-c2-str1", null ) )
		);

		assertThat( getAuditReader().find( DefaultValueComponentTestEntity.class, id6, 2 ), equalTo( ver2 ) );
	}

	private void checkCorrectlyPersisted(Integer expectedId, String expectedComp2Str1Rev1, String expectedComp2Str1Rev2) {
		// Verify that the entity was correctly persisted
		inTransaction(
				entityManager -> {
					final Long entCount = (Long) entityManager.createQuery(
							"select count(s) from DefaultValueComponentTestEntity s where s.id = "
									+ expectedId.toString()
					).getSingleResult();

					final Number auditCount = (Number) entityManager.createNativeQuery(
							"select count(id) from DefaultValueComponent_AUD s where s.id = "
									+ expectedId.toString()
					).getSingleResult();

					final String comp2Str1Rev1 = (String) entityManager
							.createNativeQuery(
									"select COMP2_STR1 from DefaultValueComponent_AUD s where REV=1 and s.id = "
											+ expectedId.toString()
							).getSingleResult();

					final String comp2Str1Rev2 = (String) entityManager
							.createNativeQuery(
									"select COMP2_STR1 from DefaultValueComponent_AUD s where REV=2 and s.id = "
											+ expectedId.toString()
							).getSingleResult();

					assertThat( entCount, equalTo( 1L ) );
					assertThat( auditCount.intValue(), equalTo( 2 ) );

					assertThat( comp2Str1Rev1, expectedComp2Str1Rev1 == null ? nullValue() : equalTo( expectedComp2Str1Rev1 ) );
					assertThat( comp2Str1Rev2, expectedComp2Str1Rev1 == null ? nullValue() : equalTo( expectedComp2Str1Rev2 ) );
				}
		);
	}
}
