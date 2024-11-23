/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.query;

import java.util.List;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Jan Schatteman
 */
@TestForIssue( jiraKey = "HHH-15559" )
public class NonWhereCriteriaTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Phone.class
		};
	}

	@Before
	public void prepareTestData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					Phone phone1 = new Phone();
					phone1.setSynced( true );
					Phone phone2 = new Phone();
					phone2.setSynced( true );
					entityManager.persist( phone1 );
					entityManager.persist( phone2 );
				}
		);
	}

	@After
	public void cleanupTestData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					entityManager.createQuery( "delete from Phone" ).executeUpdate();
				}
		);
	}

	@Test
	public void testNonWhereCriteriaUpdate() {
		doInJPA(
				this::entityManagerFactory,
				(entityManager) -> {
					CriteriaUpdate<Phone> updateCriteria = entityManager.getCriteriaBuilder().createCriteriaUpdate( Phone.class );
					updateCriteria.from( Phone.class );
					updateCriteria.set( Phone_.isSynced, Boolean.FALSE );
					entityManager.createQuery( updateCriteria ).executeUpdate();
				}
		);

		doInJPA(
				this::entityManagerFactory,
				(entityManager) -> {
					List results = entityManager.createQuery( "from Phone p where p.isSynced is false" ).getResultList();
					Assert.assertEquals( 2, results.size() );
				}
		);
	}

}
