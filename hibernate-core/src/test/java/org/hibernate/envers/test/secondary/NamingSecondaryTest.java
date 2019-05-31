/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.secondary;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.secondary.SecondaryNamingTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NamingSecondaryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SecondaryNamingTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					SecondaryNamingTestEntity ste = new SecondaryNamingTestEntity( "a", "1" );
					entityManager.persist( ste );

					this.id = ste.getId();
				},

				// Revision 2
				entityManager -> {
					SecondaryNamingTestEntity ste = entityManager.find( SecondaryNamingTestEntity.class, id );
					ste.setS1( "b" );
					ste.setS2( "2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SecondaryNamingTestEntity.class, id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId() {
		SecondaryNamingTestEntity ver1 = new SecondaryNamingTestEntity( id, "a", "1" );
		SecondaryNamingTestEntity ver2 = new SecondaryNamingTestEntity( id, "b", "2" );

		assertThat( getAuditReader().find( SecondaryNamingTestEntity.class, id, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( SecondaryNamingTestEntity.class, id, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	@Disabled("NYI - Requires secondary table support - this method requires additional porting")
	public void testTableNames() {
//		assert "sec_versions".equals(
//				((Iterator<Join>)
//						metadata().getEntityBinding(
//								"org.hibernate.envers.test.integration.secondary.SecondaryNamingTestEntity_AUD"
//						)
//								.getJoinIterator())
//						.next().getTable().getName()
//		);
	}
}
