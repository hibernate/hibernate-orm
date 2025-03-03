/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;


import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Max Rydahl Andersen
 */
public abstract class UserCollectionTypeTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	public void testBasicOperation() {
		User u = new User( 1, "max" );
		inTransaction(
				s -> {
					u.getEmailAddresses().add( new Email("max@hibernate.org") );
					u.getEmailAddresses().add( new Email("max.andersen@jboss.com") );
					s.persist(u);
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					criteria.from( User.class );
					User u2 = s.createQuery( criteria ).uniqueResult();
//					User u2 = (User) s.createCriteria(User.class).uniqueResult();
					checkEmailAddressInitialization( u2 );
					assertEquals( u2.getEmailAddresses().size(), 2 );

				}
		);

		inTransaction(
				s -> {
					User u2 = s.get( User.class, u.getId() );
					u2.getEmailAddresses().size();
					assertEquals( 2, MyListType.lastInstantiationRequest );

				}
		);

		inTransaction(
				s -> s.remove( u )
		);
	}

	protected abstract void checkEmailAddressInitialization(User user);

}
