/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles.join.selfReferencing;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class JoinSelfReferentialFetchProfileTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	@Test
	public void testEnablingJoinFetchProfileAgainstSelfReferentialAssociation() {
		inTransaction( s-> {
			s.enableFetchProfile( Employee.FETCH_PROFILE_TREE );
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Employee> criteria = criteriaBuilder.createQuery( Employee.class );
			Root<Employee> root = criteria.from( Employee.class );
			criteria.where( criteriaBuilder.isNull( root.get( "manager" ) ) );
			s.createQuery( criteria ).list();
//		s.createCriteria( Employee.class )
//				.add( Restrictions.isNull( "manager" ) )
//				.list();
		} );
	}
}
