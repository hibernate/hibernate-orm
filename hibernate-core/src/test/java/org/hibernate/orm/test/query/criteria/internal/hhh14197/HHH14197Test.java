/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh14197;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-14197")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@Jpa(
		annotatedClasses = {
				Department.class,
				Employee.class
		}
)
public class HHH14197Test {

	@Test
	public void testValidSQLGenerated(EntityManagerFactoryScope scope) {
		// without fixing HHH-14197, invalid SQL would be generated without root
		// "... where exists (select employee0_.id as id1_1_ from  where ...) ... "
		scope.inTransaction( entityManager -> {
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
