/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh13908;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@RequiresDialect( value = MySQLDialect.class, strictMatching = true )
public class HHH13908Test extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	@Test
	@JiraKey( value = "HHH-13908" )
	public void testTimeFunctionNotThrowException() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
			final Root<Foo> foo = cq.from( Foo.class );
			cq.select( foo )
					.where(
							cb.lessThanOrEqualTo(
									cb.function( "TIME", String.class, foo.get( Foo_.startTime ) ),
									"17:00:00"
							)
					);
			// without fixing, the following exception will be thrown:
			// Parameter value [17:00:00] did not match expected type [java.util.Date (n/a)]
			//java.lang.IllegalArgumentException: Parameter value [17:00:00] did not match expected type [java.util.Date (n/a)]
			entityManager.createQuery( cq ).getResultList();
		} );
	}
}
