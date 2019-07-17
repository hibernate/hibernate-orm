/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.basic;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		User u = new User("max");
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
					assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
					assertEquals( u2.getEmailAddresses().size(), 2 );

				}
		);

		inTransaction(
				s -> {
					User u2 = s.get( User.class, u.getUserName() );
					u2.getEmailAddresses().size();
					assertEquals( 2, MyListType.lastInstantiationRequest );

				}
		);

		inTransaction(
				s -> s.delete( u )
		);
	}

}

