package org.hibernate.query.criteria.internal.hhh14197;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14197" )
public class HHH14197Test extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Department.class,
				Employee.class
		};
	}

	@Test
	public void testValidSQLGenerated() {
		// without fixing HHH-14197, invalid SQL would be generated without root
		// "... where exists (select employee0_.id as id1_1_ from  where ...) ... "
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Employee> query = cb.createQuery( Employee.class );
			final Root<Employee> employee = query.from( Employee.class );

			final Subquery<Employee> subquery1 = query.subquery( Employee.class );
			final Root<Employee> employee2 = subquery1.correlate( employee );
			final SetJoin<Employee, Employee> directReport = employee2.join( Employee_.directReports );

			final Subquery<Employee> subquery2 = subquery1.subquery( Employee.class );
			final SetJoin<Employee, Employee> directReport2 = subquery2.correlate( directReport );
			directReport2.join( Employee_.annotations );

			subquery2.select( directReport2 );

			subquery1.select( employee2 ).where( cb.exists( subquery2 ) );

			query.select( employee ).where( cb.exists( subquery1 ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

}
