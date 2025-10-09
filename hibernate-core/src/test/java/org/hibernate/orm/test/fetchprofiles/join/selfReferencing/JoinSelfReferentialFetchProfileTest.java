/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles.join.selfReferencing;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = Employee.class)
@SessionFactory
public class JoinSelfReferentialFetchProfileTest {
	@Test
	public void testEnablingJoinFetchProfileAgainstSelfReferentialAssociation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( s-> {
			s.enableFetchProfile( Employee.FETCH_PROFILE_TREE );
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Employee> criteria = criteriaBuilder.createQuery( Employee.class );
			Root<Employee> root = criteria.from( Employee.class );
			criteria.where( criteriaBuilder.isNull( root.get( "manager" ) ) );
			s.createQuery( criteria ).list();
		} );
	}
}
