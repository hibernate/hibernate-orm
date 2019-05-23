/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.bidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetoone.BidirectionalEagerAnnotationRefEdOneToOne;
import org.hibernate.envers.test.support.domains.onetoone.BidirectionalEagerAnnotationRefIngOneToOne;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Erik-Berndt Scheper
 */
@TestForIssue(jiraKey = "HHH-3854")
public class BidirectionalEagerAnnotationTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer refIngId1 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BidirectionalEagerAnnotationRefEdOneToOne.class,
				BidirectionalEagerAnnotationRefIngOneToOne.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				// Revision 1
				entityManager -> {
					BidirectionalEagerAnnotationRefEdOneToOne ed1 = new BidirectionalEagerAnnotationRefEdOneToOne();
					ed1.setData( "referredEntity1" );

					BidirectionalEagerAnnotationRefIngOneToOne ing1 = new BidirectionalEagerAnnotationRefIngOneToOne();
					ing1.setData( "referringEntity" );

					// Make association
					ed1.setRefIng( ing1 );
					ing1.setRefedOne( ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ing1 );

					refIngId1 = ing1.getId();
				}
		);
	}

	@DynamicTest
	public void testNonProxyObjectTraversing() {
		BidirectionalEagerAnnotationRefIngOneToOne referencing =
				getAuditReader().find( BidirectionalEagerAnnotationRefIngOneToOne.class, refIngId1, 1 );
		assertThat( referencing.getRefedOne().getData(), notNullValue() );
	}
}