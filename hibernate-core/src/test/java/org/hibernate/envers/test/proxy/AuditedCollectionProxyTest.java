/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.proxy;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ListRefIngEntity;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Test case for HHH-5750: Proxied objects lose the temporary session used to
 * initialize them.
 *
 * @author Erik-Berndt Scheper
 */
@TestForIssue(jiraKey="HHH-5750")
public class AuditedCollectionProxyTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	Integer referencedEntityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ListRefEdEntity.class, ListRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					ListRefEdEntity referenced = new ListRefEdEntity( 1, "str1" );
					ListRefIngEntity referring = new ListRefIngEntity( 1, "refing1", referenced );

					entityManager.persist( referenced );
					entityManager.persist( referring );

					referencedEntityId = referenced.getId();
				},

				entityManager -> {
					ListRefEdEntity referenced = entityManager.find( ListRefEdEntity.class, referencedEntityId );
					ListRefIngEntity referring = new ListRefIngEntity( 2, "refing2", referenced );
					entityManager.persist( referring );
				}
		);
	}

	@DynamicTest
	public void testProxyIdentifier() {
		inTransaction(
				entityManager -> {
					ListRefEdEntity referenced = entityManager.getReference( ListRefEdEntity.class, referencedEntityId );
					assertThat( referenced, instanceOf( HibernateProxy.class ) );

					ListRefIngEntity referring = new ListRefIngEntity( 3, "refing3", referenced );
					entityManager.persist( referring );

					referenced.getReffering().size();
				}
		);
	}

}
