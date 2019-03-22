/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids;

import java.util.Date;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.DateIdTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DateIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Date id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { DateIdTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final DateIdTestEntity dite = new DateIdTestEntity( new Date(), "x" );
					entityManager.persist( dite );

					id1 = dite.getId();
				},

				// Revision 2
				entityManager -> {
					final DateIdTestEntity dite = entityManager.find( DateIdTestEntity.class, id1 );
					dite.setStr1( "y" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( DateIdTestEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		DateIdTestEntity ver1 = new DateIdTestEntity( id1, "x" );
		DateIdTestEntity ver2 = new DateIdTestEntity( id1, "y" );

		assertThat( getAuditReader().find( DateIdTestEntity.class, id1, 1 ).getStr1(), equalTo( ver1.getStr1() ) );
		assertThat( getAuditReader().find( DateIdTestEntity.class, id1, 2 ).getStr1(), equalTo( ver2.getStr1() ) );
	}
}