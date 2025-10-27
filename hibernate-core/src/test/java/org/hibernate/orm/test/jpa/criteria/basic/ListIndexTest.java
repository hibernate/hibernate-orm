/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Address_;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;

import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests usage of {@link ListJoin#index()}
 *
 * @author Brett Meyer
 */
@Jpa(annotatedClasses = {Phone.class, Address.class})
public class ListIndexTest {

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-8404")
	public void testListIndex(EntityManagerFactoryScope scope) {
		final Address address1 = new Address();
		scope.inTransaction( entityManager -> {
			address1.setId( "a1" );
			Phone phone1 = new Phone();
			phone1.setId( "p1" );
			phone1.setAddress( address1 );
			Phone phone2 = new Phone();
			phone2.setId( "p2" );

			phone2.setAddress( address1 );
			address1.getPhones().add( phone1 );
			address1.getPhones().add( phone2 );

			Address address2 = new Address();
			address2.setId( "a2" );
			Phone phone3 = new Phone();
			phone3.setId( "p3" );

			phone3.setAddress( address2 );
			address2.getPhones().add( phone3 );

			entityManager.persist( phone1 );
			entityManager.persist( phone2 );
			entityManager.persist( phone3 );
			entityManager.persist( address1 );
			entityManager.persist( address2 );
		} ) ;
		scope.inEntityManager( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Address> criteria = cb.createQuery( Address.class );
			Root<Address> addressRoot = criteria.from( Address.class );
			ListJoin<Address, Phone> phones = addressRoot.join( Address_.phones );
			criteria.where( cb.gt( phones.index(), 0 ) );
			List<Address> results = entityManager.createQuery( criteria ).getResultList();

			assertNotNull( results );
			// Ensure that the "index(phones) > 0" condition was included on the inner join, meaning only address1
			// (> 1 phone) was returned.
			assertEquals( 1, results.size() );
			assertEquals( address1.getId(), results.get( 0 ).getId() );
		} );
	}
}
