/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.onetomany;

import java.util.Collections;
import java.util.HashSet;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.SetRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InverseSideChangesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;

	private Integer ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetRefEdEntity.class, SetRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
					entityManager.persist( ed1 );
					ed1_id = ed1.getId();
				},

				// Revision 2
				entityManager -> {
					SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );
					entityManager.persist( ing1 );
					ing1_id = ing1.getId();

					SetRefEdEntity ed1 = entityManager.find( SetRefEdEntity.class, ed1_id );
					ed1.setReffering( new HashSet<>() );
					ed1.getReffering().add( ing1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ), contains( 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		final SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );
		assertThat( rev1.getReffering(), equalTo( Collections.EMPTY_SET ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		final SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );
		assertThat( rev2.getReference(), nullValue() );
	}
}