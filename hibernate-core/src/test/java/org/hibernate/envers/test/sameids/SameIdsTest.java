/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.sameids;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.sameids.SameIdTestEntity1;
import org.hibernate.envers.test.support.domains.sameids.SameIdTestEntity2;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * A test which checks that if we add two different entities with the same ids in one revision, they
 * will both be stored.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class SameIdsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SameIdTestEntity1.class, SameIdTestEntity2.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					final SameIdTestEntity1 site1 = new SameIdTestEntity1( 1, "str1" );
					final SameIdTestEntity2 site2 = new SameIdTestEntity2( 1, "str1" );

					entityManager.persist( site1 );
					entityManager.persist( site2 );
				},

				entityManager -> {
					final SameIdTestEntity1 site1 = entityManager.find( SameIdTestEntity1.class, 1 );
					final SameIdTestEntity2 site2 = entityManager.find( SameIdTestEntity2.class, 1 );
					site1.setStr1( "str2" );
					site2.setStr1( "str2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SameIdTestEntity1.class, 1 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( SameIdTestEntity2.class, 1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfSite1() {
		SameIdTestEntity1 ver1 = new SameIdTestEntity1( 1, "str1" );
		SameIdTestEntity1 ver2 = new SameIdTestEntity1( 1, "str2" );

		assertThat( getAuditReader().find( SameIdTestEntity1.class, 1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( SameIdTestEntity1.class, 1, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfSite2() {
		SameIdTestEntity2 ver1 = new SameIdTestEntity2( 1, "str1" );
		SameIdTestEntity2 ver2 = new SameIdTestEntity2( 1, "str2" );

		assertThat( getAuditReader().find( SameIdTestEntity2.class, 1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( SameIdTestEntity2.class, 1, 2 ), equalTo( ver2 ) );
	}
}
