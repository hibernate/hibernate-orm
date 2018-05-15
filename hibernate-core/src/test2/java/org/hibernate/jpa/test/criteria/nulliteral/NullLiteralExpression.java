/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.nulliteral;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class NullLiteralExpression extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Subject.class};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11159")
	public void testNullLiteralExpressionInCriteriaUpdate() {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaUpdate<Person> criteriaUpdate = builder.createCriteriaUpdate( Person.class );
			criteriaUpdate.from(Person.class);
			criteriaUpdate.set( Person_.subject, builder.nullLiteral( Subject.class ) );

			entityManager.createQuery( criteriaUpdate ).executeUpdate();

			entityManager.getTransaction().commit();
		}
		catch (Exception e) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}
}
