/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Max Rydahl Andersen
 * @author David Weinberg
 */
@SessionFactory
public abstract class UserCollectionTypeTest {

	@Test
	public void testBasicOperation(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					User u = new User( 1, "max" );
					u.getEmailAddresses().add( new Email("max@hibernate.org") );
					u.getEmailAddresses().add( new Email("max.andersen@jboss.com") );
					s.persist(u);
				}
		);

		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					criteria.from( User.class );
					User u2 = s.createQuery( criteria ).uniqueResult();
					checkEmailAddressInitialization(u2);
					assertEquals( 2, u2.getEmailAddresses().size() );
					assertNotNull( u2.getEmailAddresses().head());
				}
		);
	}

	protected abstract void checkEmailAddressInitialization(User user);

}
