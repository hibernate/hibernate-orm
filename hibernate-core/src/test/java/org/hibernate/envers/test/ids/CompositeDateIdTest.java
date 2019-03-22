/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids;

import java.util.Date;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.CompositeDateIdTestEntity;
import org.hibernate.envers.test.support.domains.ids.DateEmbId;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeDateIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private DateEmbId id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CompositeDateIdTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final DateEmbId dateEmbId = new DateEmbId( new Date(), new Date() );
					final CompositeDateIdTestEntity dite = new CompositeDateIdTestEntity( dateEmbId, "x" );

					entityManager.persist( dite );
					id1 = dite.getId();
				},

				// Revision 2
				entityManager -> {
					final CompositeDateIdTestEntity dite = entityManager.find( CompositeDateIdTestEntity.class, id1 );
					dite.setStr1( "y" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( CompositeDateIdTestEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		CompositeDateIdTestEntity ver1 = new CompositeDateIdTestEntity( id1, "x" );
		CompositeDateIdTestEntity ver2 = new CompositeDateIdTestEntity( id1, "y" );

		assertThat( getAuditReader().find( CompositeDateIdTestEntity.class, id1, 1 ).getStr1(), equalTo( "x" ) );
		assertThat( getAuditReader().find( CompositeDateIdTestEntity.class, id1, 2 ).getStr1(), equalTo( "y" ) );
	}
}