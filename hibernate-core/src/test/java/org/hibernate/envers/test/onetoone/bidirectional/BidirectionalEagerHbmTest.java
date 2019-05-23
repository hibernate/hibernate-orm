/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.bidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetoone.BidirectionalEagerHbmRefEdPK;
import org.hibernate.envers.test.support.domains.onetoone.BidirectionalEagerHbmRefIngPK;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Erik-Berndt Scheper, Amar Singh
 */
@TestForIssue(jiraKey = "HHH-3854")
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class BidirectionalEagerHbmTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long refIngId1 = null;

	@Override
	protected String[] getMappings() {
		return new String[] { "oneToOne/bidirectional/eagerLoading.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				// Revision 1
				entityManager -> {
					BidirectionalEagerHbmRefEdPK ed1 = new BidirectionalEagerHbmRefEdPK( "data_ed_1" );
					BidirectionalEagerHbmRefIngPK ing1 = new BidirectionalEagerHbmRefIngPK( "data_ing_1" );
					ing1.setReference( ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ing1 );

					refIngId1 = ing1.getId();
				}
		);
	}

	@DynamicTest
	public void testNonProxyObjectTraversing() {
		BidirectionalEagerHbmRefIngPK referencing =
				getAuditReader().find( BidirectionalEagerHbmRefIngPK.class, refIngId1, 1 );
		assertThat( referencing.getReference().getData(), notNullValue() );
	}
}