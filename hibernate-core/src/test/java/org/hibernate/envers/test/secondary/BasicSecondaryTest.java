/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.secondary;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.secondary.SecondaryTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSecondaryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SecondaryTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					SecondaryTestEntity ste = new SecondaryTestEntity( "a", "1" );
					entityManager.persist( ste );

					this.id = ste.getId();
				},

				// Revision 2
				entityManager -> {
					SecondaryTestEntity ste = entityManager.find( SecondaryTestEntity.class, id );
					ste.setS1( "b" );
					ste.setS2( "2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SecondaryTestEntity.class, id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId() {
		SecondaryTestEntity ver1 = new SecondaryTestEntity( id, "a", "1" );
		SecondaryTestEntity ver2 = new SecondaryTestEntity( id, "b", "2" );

		assertThat( getAuditReader().find( SecondaryTestEntity.class, id, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( SecondaryTestEntity.class, id, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	@Disabled("NYI - Requires secondary table support - this method requires additional porting")
	public void testTableNames() {
//		assert "secondary_AUD".equals(
//				((Iterator<Join>)
//						metadata().getEntityBinding(
//								"org.hibernate.envers.test.integration.secondary.SecondaryTestEntity_AUD"
//						)
//								.getJoinIterator())
//						.next().getTable().getName()
//		);
	}
}